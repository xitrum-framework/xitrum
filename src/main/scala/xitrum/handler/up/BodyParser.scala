package xitrum.handler.up

import java.nio.charset.Charset
import scala.collection.mutable.{Map => MMap}
import scala.util.control.NonFatal

import org.jboss.netty.channel.{Channel, ChannelHandler, SimpleChannelUpstreamHandler, ChannelHandlerContext, ChannelStateEvent, MessageEvent, ExceptionEvent, Channels}
import org.jboss.netty.handler.codec.http.{DefaultHttpResponse, HttpHeaders, HttpMethod, HttpRequest, HttpChunk, HttpResponseStatus, HttpVersion}
import org.jboss.netty.handler.codec.http.multipart.{Attribute, DefaultHttpDataFactory, DiskAttribute, DiskFileUpload, FileUpload, HttpPostRequestDecoder, InterfaceHttpData, HttpData}
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

  // Limit each field, including file upload
  if (Config.xitrum.request.maxSizeInBytes > 0)
    factory.setMaxLimit(Config.xitrum.request.maxSizeInBytes)
}

// Based on the file upload example in Netty.
class BodyParser extends SimpleChannelUpstreamHandler with BadClientSilencer {
  import BodyParser._

  private[this] var env: HandlerEnv = _
  private[this] var readingChunks   = false
  private[this] var bytesReceived   = 0L

  override def channelClosed(ctx: ChannelHandlerContext, e: ChannelStateEvent) {
    if (env != null && env.bodyDecoder != null) {
      env.bodyDecoder.cleanFiles()
      env.bodyDecoder = null
    }
  }

  override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {
    val m       = e.getMessage
    val channel = ctx.getChannel
    try {
      if (m.isInstanceOf[HttpRequest] && !readingChunks) {
        handleHttpRequest(ctx, m.asInstanceOf[HttpRequest])
      } else if (m.isInstanceOf[HttpChunk] && readingChunks) {
        handleHttpChunk(ctx, m.asInstanceOf[HttpChunk])
      } else {
        ctx.sendUpstream(e)
      }
    } catch {
      case NonFatal(e) =>
        val msg = "Could not parse content body of request: " + m
        log.warn(msg, e)
        channel.close()
    }
  }

  //----------------------------------------------------------------------------

  private def handleHttpRequest(ctx: ChannelHandlerContext, request: HttpRequest) {
    // See ChannelPipelineFactory
    // This is the first Xitrum handler, log the request
    if (log.isTraceEnabled) log.trace(request.toString)

    // Clean previous files if any
    if (env != null && env.bodyDecoder != null) env.bodyDecoder.cleanFiles()

    val channel        = ctx.getChannel
    env                = new HandlerEnv
    env.channel        = channel
    env.bodyDecoder    = new HttpPostRequestDecoder(factory, request)
    env.bodyTextParams = MMap.empty[String, Seq[String]]
    env.bodyFileParams = MMap.empty[String, Seq[FileUpload]]
    env.request        = request
    env.response       = {  // The default response is empty 200 OK
      // http://en.wikipedia.org/wiki/HTTP_persistent_connection
      // In HTTP 1.1 all connections are considered persistent unless declared otherwise
      val ret = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK)
      HttpHeaders.setContentLength(ret, 0)
      ret
    }

    bytesReceived = 0
    if (request.isChunked) {
      readingChunks = true
    } else if (readHttpDataAllReceive()) {
      Channels.fireMessageReceived(ctx, env)
    } else {
      warnBigRequest(channel)
    }
  }

  private def handleHttpChunk(ctx: ChannelHandlerContext, chunk: HttpChunk) {
    val channel = ctx.getChannel

    env.bodyDecoder.offer(chunk)
    if (!readHttpDataChunkByChunk()) warnBigRequest(channel)

    if (chunk.isLast) {
      if (readHttpDataAllReceive()) {
        Channels.fireMessageReceived(ctx, env)
        readingChunks = false
      } else {
        warnBigRequest(channel)
      }
    }
  }

  private def readHttpDataChunkByChunk(): Boolean = {
    try {
      var sizeOk = true
      while (sizeOk && env.bodyDecoder.hasNext()) {
        val data = env.bodyDecoder.next()
        if (data != null) {
          sizeOk = checkSize(data)
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

  // May throw exception on decode error
  private def readHttpDataAllReceive(): Boolean = {
    val method = env.request.getMethod
    if (!method.equals(HttpMethod.POST) &&
        !method.equals(HttpMethod.PUT) &&
        !method.equals(HttpMethod.PATCH))
      return true

    val requestContentType = HttpHeaders.getHeader(env.request, HttpHeaders.Names.CONTENT_TYPE)
    if (requestContentType == null) return true

    val requestContentTypeLowerCase = requestContentType.toLowerCase
    if (!requestContentTypeLowerCase.startsWith(HttpHeaders.Values.APPLICATION_X_WWW_FORM_URLENCODED) &&
        !requestContentTypeLowerCase.startsWith(HttpHeaders.Values.MULTIPART_FORM_DATA))
      return true

    val datas  = env.bodyDecoder.getBodyHttpDatas
    val it     = datas.iterator
    var sizeOk = true
    while (sizeOk && it.hasNext()) {
      val data = it.next()
      sizeOk = checkSize(data)
      if (sizeOk) putDataToEnv(data)
    }
    sizeOk
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
  private def checkSize(data: InterfaceHttpData): Boolean = {
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

  private def warnBigRequest(channel: Channel) {
    val msg = "Request content body is too big, see xitrum.request.maxSizeInMB in xitrum.conf"
    log.warn(msg)
    channel.close()
  }
}
