package xitrum.handler.inbound

import java.nio.charset.Charset
import scala.collection.mutable.{Map => MMap}
import scala.util.control.NonFatal

import io.netty.buffer.Unpooled
import io.netty.channel.{SimpleChannelInboundHandler, ChannelFutureListener, ChannelHandlerContext}
import io.netty.handler.codec.http.{
  HttpRequest, FullHttpRequest, FullHttpResponse, DefaultFullHttpRequest, DefaultFullHttpResponse,
  HttpHeaders, HttpContent, HttpObject, LastHttpContent, HttpResponseStatus, HttpVersion
}
import io.netty.handler.codec.http.multipart.{
  Attribute, DiskAttribute,
  FileUpload, DiskFileUpload,
  DefaultHttpDataFactory, HttpPostRequestDecoder, InterfaceHttpData, HttpData
}
import InterfaceHttpData.HttpDataType

import xitrum.{Config, Log}
import xitrum.handler.HandlerEnv
import xitrum.scope.request.{FileUploadParams, Params, PathInfo}
import xitrum.util.ByteBufUtil

object Request2Env {
  DiskAttribute.deleteOnExitTemporaryFile  = true  // Should delete file on exit (in normal exit)
  DiskAttribute.baseDirectory              = Config.xitrum.request.tmpUploadDir

  DiskFileUpload.deleteOnExitTemporaryFile = true  // Should delete file on exit (in normal exit)
  DiskFileUpload.baseDirectory             = Config.xitrum.request.tmpUploadDir

  // Creating factory should be after the above for the factory to take effect of the settings

  // https://github.com/ngocdaothanh/xitrum/issues/77
  // Save a field to disk if its size exceeds maxSizeInBytesOfUploadMem
  val factory = new DefaultHttpDataFactory(Config.xitrum.request.maxSizeInBytesOfUploadMem)
}

/**
 * This handler converts request with its content body (if any, e.g. in case of
 * file upload) to HandlerEnv, and send it upstream to the next handler.
 */
class Request2Env extends SimpleChannelInboundHandler[HttpObject] with Log {
  // Based on the file upload example in Netty.

  import Request2Env._

  private[this] var env: HandlerEnv   = null   // Will be reset to null after being sent upstream to the next handler
  private[this] var bodyBytesReceived = 0L     // For checking if the body is too big, bigger than Config.xitrum.request.maxSizeInBytes

  override def channelInactive(ctx: ChannelHandlerContext) {
    // In case the connection is closed when the request is not fully received,
    // thus env is initialized but not sent upstream to the next handler
    release()
  }

  override def channelRead0(ctx: ChannelHandlerContext, msg: HttpObject) {
    if (msg.getDecoderResult.isFailure) {
      ctx.channel.close()
      return
    }

    try {
      if (msg.isInstanceOf[HttpRequest]) {
        handleHttpRequestHead(ctx, msg.asInstanceOf[HttpRequest])
      } else {
        if (msg.isInstanceOf[HttpContent])
          handleHttpRequestContent(ctx, msg.asInstanceOf[HttpContent])

        // LastHttpContent is a HttpContent.
        // env may be set to null at handleHttpRequestContent above, when
        // closeOnBigRequest is called.
        if (env != null && msg.isInstanceOf[LastHttpContent])
          sendUpstream(ctx)
      }
    } catch {
      case NonFatal(e) =>
        val m = "Could not parse content body of request: " + msg
        log.warn(m, e)
        ctx.channel.close()
    }
  }

  //----------------------------------------------------------------------------

  private def handleHttpRequestHead(ctx: ChannelHandlerContext, request: HttpRequest) {
    // See DefaultHttpChannelInitializer
    // This is the first Xitrum handler, log the request
    if (log.isTraceEnabled) log.trace(request.toString)

    // Clean previous files if any
    release()

    env                = new HandlerEnv
    env.channel        = ctx.channel
    env.bodyTextParams = MMap.empty[String, Seq[String]]
    env.bodyFileParams = MMap.empty[String, Seq[FileUpload]]
    env.request        = createEmptyFullHttpRequest(request)
    env.response       = createEmptyFullResponse(request)

    bodyBytesReceived = 0
    try {
      // Otherwise env.bodyDecoder is null (see HandlerEnv's constructor)
      if (isAPPLICATION_X_WWW_FORM_URLENCODED_or_MULTIPART_FORM_DATA(request))
        env.bodyDecoder = new HttpPostRequestDecoder(factory, request)
    } catch {
      case e: HttpPostRequestDecoder.ErrorDataDecoderException =>
        ctx.channel.close()

      case e: HttpPostRequestDecoder.IncompatibleDataDecoderException =>
        sendUpstream(ctx)
    }
  }

  private def handleHttpRequestContent(ctx: ChannelHandlerContext, content: HttpContent) {
    // To save memory, only set env.request.content when env.bodyDecoder is not in action
    if (env.bodyDecoder == null) {
      val body   = content.content
      val length = body.readableBytes
      if (bodyBytesReceived + length <= Config.xitrum.request.maxSizeInBytes) {
        env.request.content.writeBytes(body)
        bodyBytesReceived += length
      } else {
        closeOnBigRequest(ctx)
      }
    } else {
      env.bodyDecoder.offer(content)
      if (!readHttpDataChunkByChunk()) closeOnBigRequest(ctx)
    }
  }

  //----------------------------------------------------------------------------

  private def release() {
    if (env != null) {
      if (env.bodyDecoder != null) {
        env.bodyDecoder.cleanFiles()
        env.bodyDecoder.destroy()
      }

      env.request.release()
      env.response.release()

      env = null
    }
  }

  private def createEmptyFullHttpRequest(request: HttpRequest): FullHttpRequest = {
    val ret = new DefaultFullHttpRequest(request.getProtocolVersion, request.getMethod, request.getUri)
    ret.headers.set(request.headers)
    ret
  }

  private def createEmptyFullResponse(request: HttpRequest): FullHttpResponse = {
    // https://github.com/netty/netty/issues/2137
    val compositeBuf = Unpooled.compositeBuffer(1)

    // In HTTP 1.1 all connections are considered persistent unless declared otherwise
    // http://en.wikipedia.org/wiki/HTTP_persistent_connection
    val ret = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, compositeBuf)

    // Unless the Connection: keep-alive header is present in the HTTP response,
    // apache benchmark (ab) hangs on keep alive connections
    // https://github.com/veebs/netty/commit/64f529945282e41eb475952fde382f234da8eec7
    if (HttpHeaders.isKeepAlive(request))
      HttpHeaders.setHeader(ret, HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE)

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
    bodyBytesReceived + hd.length <= Config.xitrum.request.maxSizeInBytes
  }

  private def putDataToEnv(data: InterfaceHttpData) {
    val dataType = data.getHttpDataType
    if (dataType == HttpDataType.Attribute) {
      val attribute = data.asInstanceOf[Attribute]
      val name      = attribute.getName
      val value     = attribute.getValue
      putOrAppendString(env.bodyTextParams, name, value)
      bodyBytesReceived += attribute.length
    } else if (dataType == HttpDataType.FileUpload) {
      val fileUpload = data.asInstanceOf[FileUpload]
      val length     = fileUpload.length
      if (fileUpload.isCompleted && length > 0) {  // Skip empty file
        val name = fileUpload.getName
        sanitizeFileUploadFilename(fileUpload)
        putOrAppendFileUpload(env.bodyFileParams, name, fileUpload)
        bodyBytesReceived += length
      }
    }
  }

  private def closeOnBigRequest(ctx: ChannelHandlerContext) {
    val response = env.response
    response.setStatus(HttpResponseStatus.BAD_REQUEST)
    ByteBufUtil.writeComposite(
      response.content,
      Unpooled.copiedBuffer("Request content body is too big", Config.xitrum.request.charset)
    )
    ctx.channel.write(env).addListener(ChannelFutureListener.CLOSE)

    log.warn("Request content body is too big, see xitrum.request.maxSizeInMB in xitrum.conf")

    // Mark that closeOnBigRequest has been called.
    // See the check for LastHttpContent above.
    env = null
  }

  private def sendUpstream(ctx: ChannelHandlerContext) {
    ctx.fireChannelRead(env)

    // Reset for the next request on this same connection (e.g. keep alive)
    env               = null
    bodyBytesReceived = 0
  }
}
