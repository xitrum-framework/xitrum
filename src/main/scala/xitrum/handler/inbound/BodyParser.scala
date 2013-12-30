package xitrum.handler.inbound

import java.nio.charset.Charset
import scala.collection.mutable.{Map => MMap}
import scala.util.control.NonFatal

import io.netty.channel.{SimpleChannelInboundHandler, ChannelHandlerContext}
import io.netty.handler.codec.http.{
  HttpRequest, FullHttpRequest, DefaultFullHttpRequest,
  FullHttpResponse, DefaultFullHttpResponse,
  HttpHeaders, HttpContent, HttpObject, LastHttpContent, HttpResponseStatus, HttpVersion
}
import io.netty.handler.codec.http.multipart.{
  Attribute, DiskAttribute,
  FileUpload, DiskFileUpload,
  DefaultHttpDataFactory, HttpPostRequestDecoder, InterfaceHttpData, HttpData
}
import InterfaceHttpData.HttpDataType

import xitrum.Config
import xitrum.handler.HandlerEnv
import xitrum.scope.request.{FileUploadParams, Params, PathInfo}

object BodyParser {
  DiskAttribute.deleteOnExitTemporaryFile  = true  // Should delete file on exit (in normal exit)
  DiskAttribute.baseDirectory              = Config.xitrum.request.tmpUploadDir

  DiskFileUpload.deleteOnExitTemporaryFile = true  // Should delete file on exit (in normal exit)
  DiskFileUpload.baseDirectory             = Config.xitrum.request.tmpUploadDir

  // Creating factory should be after the above for the factory to take effect of the settings

  // https://github.com/ngocdaothanh/xitrum/issues/77
  // Save a field to disk if its size exceeds maxSizeInBytesOfUploadMem
  val factory = new DefaultHttpDataFactory(Config.xitrum.request.maxSizeInBytesOfUploadMem)
}

// Based on the file upload example in Netty.
class BodyParser extends SimpleChannelInboundHandler[HttpObject] with BadClientSilencer {
  import BodyParser._

  private[this] var env: HandlerEnv = _
  private[this] var bytesReceived   = 0L

  override def channelInactive(ctx: ChannelHandlerContext) {
    if (env != null && env.bodyDecoder != null) {
      env.bodyDecoder.cleanFiles()
      env.bodyDecoder.destroy()
      env.bodyDecoder = null
    }
  }

  override def channelRead0(ctx: ChannelHandlerContext, msg: HttpObject) {
    try {
      if (msg.isInstanceOf[HttpRequest])
        handleHttpRequestNoncontent(ctx, msg.asInstanceOf[HttpRequest])

      if (msg.isInstanceOf[HttpContent])
        handleHttpRequestContent(ctx, msg.asInstanceOf[HttpContent])

      if (msg.isInstanceOf[LastHttpContent]) {
        ctx.fireChannelRead(env)
        env           = null
        bytesReceived = 0
      }
    } catch {
      case NonFatal(e) =>
        val m = "Could not parse content body of request: " + msg
        log.warn(m, e)
        ctx.close()
    }
  }

  //----------------------------------------------------------------------------

  private def handleHttpRequestNoncontent(ctx: ChannelHandlerContext, request: HttpRequest) {
    // See DefaultHttpChannelInitializer
    // This is the first Xitrum handler, log the request
    if (log.isTraceEnabled) log.trace(request.toString)

    // Clean previous files if any
    if (env != null && env.bodyDecoder != null) env.bodyDecoder.cleanFiles()

    env                = new HandlerEnv
    env.channel        = ctx.channel
    env.bodyTextParams = MMap.empty[String, Seq[String]]
    env.bodyFileParams = MMap.empty[String, Seq[FileUpload]]
    env.request        = createEmptyFullHttpRequest(request)
    env.response       = createEmptyFullResponse()

    bytesReceived = 0
    try {
      // Otherwise env.bodyDecoder is null (see HandlerEnv's constructor)
      if (isAPPLICATION_X_WWW_FORM_URLENCODED_or_MULTIPART_FORM_DATA(request))
        env.bodyDecoder = new HttpPostRequestDecoder(factory, request)
    } catch {
      case e: HttpPostRequestDecoder.ErrorDataDecoderException =>
        ctx.close()

      case e: HttpPostRequestDecoder.IncompatibleDataDecoderException =>
        ctx.fireChannelRead(env)
    }
  }

  private def handleHttpRequestContent(ctx: ChannelHandlerContext, content: HttpContent) {
    // To save memory, only set env.request.content when env.bodyDecoder is not in action
    if (env.bodyDecoder == null) {
      val body   = content.content
      val length = body.readableBytes
      if (bytesReceived + length <= Config.xitrum.request.maxSizeInBytes) {
        env.request.content.writeBytes(body)
        bytesReceived += length
      } else {
        closeOnBigRequest(ctx)
      }
    } else {
      env.bodyDecoder.offer(content)
      if (!readHttpDataChunkByChunk()) closeOnBigRequest(ctx)
    }
  }

  //----------------------------------------------------------------------------

  private def createEmptyFullHttpRequest(request: HttpRequest): FullHttpRequest = {
    val ret = new DefaultFullHttpRequest(request.getProtocolVersion, request.getMethod, request.getUri)
    ret.headers.set(request.headers)
    ret
  }

  private def createEmptyFullResponse(): FullHttpResponse = {
    // http://en.wikipedia.org/wiki/HTTP_persistent_connection
    // In HTTP 1.1 all connections are considered persistent unless declared otherwise
    val ret = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK)
    HttpHeaders.setContentLength(ret, 0)
    ret
  }

  private def isAPPLICATION_X_WWW_FORM_URLENCODED_or_MULTIPART_FORM_DATA(request: HttpRequest): Boolean = {
    val requestContentType = HttpHeaders.getHeader(request, HttpHeaders.Names.CONTENT_TYPE)
    if (requestContentType == null) return false

    val requestContentTypeLowerCase = requestContentType.toLowerCase
    requestContentTypeLowerCase.startsWith(HttpHeaders.Values.APPLICATION_X_WWW_FORM_URLENCODED) ||
    requestContentTypeLowerCase.startsWith(HttpHeaders.Values.MULTIPART_FORM_DATA)
  }

  private def readHttpDataChunkByChunk(): Boolean = {
    try {
      var sizeOk = true
      while (sizeOk && env.bodyDecoder.hasNext()) {
        val data = env.bodyDecoder.next()
        if (data != null) {
          sizeOk = checkHttpDataSize(data)
          if (sizeOk) putDataToEnv(data)
        }
      }
      sizeOk
    } catch {
      case e: HttpPostRequestDecoder.EndOfDataDecoderException =>
        // End
        true
    }
  }

  private def sanitizeFileUploadFilename(fileUpload: FileUpload) {
    val filename1 = fileUpload.getFilename
    val filename2 = filename1.split('/').last.split('\\').last.trim.replaceAll("^\\.+", "")
    val filename3 = if (filename2.isEmpty) "filename" else filename2
    fileUpload.setFilename(filename3)
  }

  private def putOrAppendString(map: Params, key: String, value: String) {
    if (!map.contains(key)) {
      map(key) = Seq(value)
    } else {
      val values = map(key)
      map(key) = values :+ value
    }
  }

  private def putOrAppendFileUpload(map: FileUploadParams, key: String, value: FileUpload) {
    if (!map.contains(key)) {
      map(key) = Seq(value)
    } else {
      val values = map(key)
      map(key) = values :+ value
    }
  }

  /** @return true if OK */
  private def checkHttpDataSize(data: InterfaceHttpData): Boolean = {
    val hd = data.asInstanceOf[HttpData]
    bytesReceived + hd.length <= Config.xitrum.request.maxSizeInBytes
  }

  private def putDataToEnv(data: InterfaceHttpData) {
    val dataType = data.getHttpDataType
    if (dataType == HttpDataType.Attribute) {
      val attribute = data.asInstanceOf[Attribute]
      val name      = attribute.getName
      val value     = attribute.getValue
      putOrAppendString(env.bodyTextParams, name, value)
      bytesReceived += attribute.length
    } else if (dataType == HttpDataType.FileUpload) {
      val fileUpload = data.asInstanceOf[FileUpload]
      val length     = fileUpload.length
      if (fileUpload.isCompleted && length > 0) {  // Skip empty file
        val name = fileUpload.getName
        sanitizeFileUploadFilename(fileUpload)
        putOrAppendFileUpload(env.bodyFileParams, name, fileUpload)
        bytesReceived += length
      }
    }
  }

  private def closeOnBigRequest(ctx: ChannelHandlerContext) {
    val msg = "Request content body is too big, see xitrum.request.maxSizeInMB in xitrum.conf"
    log.warn(msg)
    ctx.close()
  }
}
