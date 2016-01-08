package xitrum.handler.inbound

import java.io.File
import scala.collection.mutable.{Map => MMap}
import scala.util.control.NonFatal

import io.netty.buffer.Unpooled
import io.netty.channel.{SimpleChannelInboundHandler, ChannelHandlerContext}
import io.netty.handler.codec.http.{
  HttpRequest, FullHttpRequest, FullHttpResponse, DefaultFullHttpRequest, DefaultFullHttpResponse,
  HttpMethod, HttpHeaders, HttpContent, HttpObject, LastHttpContent, HttpResponseStatus, HttpVersion
}
import io.netty.handler.codec.http.multipart.{
  Attribute, DiskAttribute,
  FileUpload, DiskFileUpload,
  DefaultHttpDataFactory, HttpPostRequestDecoder, InterfaceHttpData, HttpData
}
import InterfaceHttpData.HttpDataType
import HttpMethod._

import xitrum.{Config, Log}
import xitrum.handler.{HandlerEnv, NoRealPipelining}
import xitrum.scope.request.{FileUploadParams, Params}

import org.json4s._
import org.json4s.jackson.JsonMethods

object Request2Env {
  // This directory must exist otherwise Netty will throw:
  // java.io.IOException: No such file or directory
  val uploadDir = new File(Config.xitrum.tmpDir, "upload")
  if (!uploadDir.exists) uploadDir.mkdir()

  DiskAttribute.baseDirectory  = uploadDir.getAbsolutePath
  DiskFileUpload.baseDirectory = uploadDir.getAbsolutePath

  // Should delete file on exit (in normal exit)
  DiskAttribute.deleteOnExitTemporaryFile  = true
  DiskFileUpload.deleteOnExitTemporaryFile = true

  // Save a field to disk if its size exceeds maxSizeInBytesOfUploadMem;
  // creating factory should be after the above for the factory to take effect of the settings
  val factory = new DefaultHttpDataFactory(Config.xitrum.request.maxSizeInBytesOfUploadMem)
}

/**
 * This handler converts request with its content body (if any, e.g. in case of
 * file upload) to HandlerEnv, and send it upstream to the next handler.
 */
class Request2Env extends SimpleChannelInboundHandler[HttpObject] {
  // Based on the file upload example in Netty.

  import Request2Env._

  private[this] var env: HandlerEnv   = null   // Will be reset to null after being sent upstream to the next handler
  private[this] var bodyBytesReceived = 0L     // For checking if the body is too big, bigger than Config.xitrum.request.maxSizeInBytes

  override def channelInactive(ctx: ChannelHandlerContext) {
    // In case the connection is closed when the request is not fully received,
    // thus env is initialized but not sent upstream to the next handler
    if (env != null) {
      env.release()
      env = null
    }
  }

  override def channelRead0(ctx: ChannelHandlerContext, msg: HttpObject) {
    val decRet = msg.getDecoderResult
    if (decRet.isFailure) {
      BadClientSilencer.respond400(ctx.channel, "Could not decode request: " + decRet.cause.getMessage)
      return
    }

    try {
      // For each request:
      // - HttpRequest (or subclass) will come first
      // - Other HttpObjects will follow
      // - LastHttpContent (or subclass) will come last
      if (msg.isInstanceOf[HttpRequest]) {
        val request = msg.asInstanceOf[HttpRequest]

        // http://www.w3.org/Protocols/rfc2616/rfc2616-sec8.html
        // curl can send "100-continue" header:
        // curl -v -X POST -F "a=b" http://server/
        if (HttpHeaders.is100ContinueExpected(request)) {
          // This request only contains headers, write response and flush
          // immediately so that the client sends the rest of the request as
          // soon as possible
          val response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE)
          ctx.channel.writeAndFlush(response)
        }

        handleHttpRequestHead(ctx, request)
      } else {
        // HttpContent can be LastHttpContent (see below)
        if (env != null && msg.isInstanceOf[HttpContent])
          handleHttpRequestContent(ctx, msg.asInstanceOf[HttpContent])

        // LastHttpContent is a HttpContent.
        // env may be set to null at handleHttpRequestContent above, when
        // closeOnBigRequest is called.
        if (env != null && msg.isInstanceOf[LastHttpContent]) {
          if (isAPPLICATION_JSON(env.request)) parseTextParamsFromJson(env)
          sendUpstream(ctx)
        }
      }
    } catch {
      case NonFatal(e) =>
        val m = "Could not parse content body of request: " + msg
        Log.warn(m, e)
        BadClientSilencer.respond400(ctx.channel, "Could not parse content body of request: " + e.getMessage)
    }
  }

  //----------------------------------------------------------------------------

  private def handleHttpRequestHead(ctx: ChannelHandlerContext, request: HttpRequest) {
    // See DefaultHttpChannelInitializer
    // This is the first Xitrum handler, log the request
    Log.trace(request.toString)

    // Clean previous files if any;
    // one connection may be used to send multiple requests,
    // so one handler instance may be used to handle multiple requests
    if (env != null) env.release()

    val method       = request.getMethod
    val bodyToDecode = (method == POST || method == PUT || method == PATCH)
    var responded400 = false
    val bodyDecoder  =
      if (bodyToDecode && isAPPLICATION_X_WWW_FORM_URLENCODED_or_MULTIPART_FORM_DATA(request)){
        try {
          new HttpPostRequestDecoder(factory, request)
        } catch {
          // Another exception is IncompatibleDataDecoderException, which means the
          // request is valid, just no need to decode (see the check above)
          case e: HttpPostRequestDecoder.ErrorDataDecoderException =>
            BadClientSilencer.respond400(ctx.channel, "Could not parse content body of request: " + e.getMessage)
            responded400 = true
            null
        }
      } else {
        null
      }

    // Only initialize env when needed
    if (!responded400) {
      env                = new HandlerEnv
      env.channel        = ctx.channel
      env.bodyTextParams = MMap.empty[String, Seq[String]]
      env.bodyFileParams = MMap.empty[String, Seq[FileUpload]]
      env.request        = createEmptyFullHttpRequest(request)
      env.response       = createEmptyFullResponse(request)
      env.bodyDecoder    = bodyDecoder
      bodyBytesReceived  = 0
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

  private def createEmptyFullHttpRequest(request: HttpRequest): FullHttpRequest = {
    val ret = new DefaultFullHttpRequest(request.getProtocolVersion, request.getMethod, request.getUri)
    ret.headers.set(request.headers)
    ret
  }

  private def createEmptyFullResponse(request: HttpRequest): FullHttpResponse = {
    // https://github.com/netty/netty/issues/2137
    val compositeBuf = Unpooled.compositeBuffer()

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
      // Do not skip empty file
      // https://github.com/xitrum-framework/xitrum/issues/463
      val fileUpload = data.asInstanceOf[FileUpload]
      if (fileUpload.isCompleted) {
        val name   = fileUpload.getName
        val length = fileUpload.length
        sanitizeFileUploadFilename(fileUpload)
        putOrAppendFileUpload(env.bodyFileParams, name, fileUpload)
        bodyBytesReceived += length
      }
    }
  }

  private def closeOnBigRequest(ctx: ChannelHandlerContext) {
    Log.warn("Request content body is too big, see xitrum.request.maxSizeInMB in xitrum.conf")
    BadClientSilencer.respond400(ctx.channel, "Request content body is too big. Limit: " + Config.xitrum.request.config.getLong("maxSizeInMB") + " bytes")

    // Mark that closeOnBigRequest has been called.
    // See the check for LastHttpContent above.
    env.release()
    env = null
  }

  private def sendUpstream(ctx: ChannelHandlerContext) {
    // NoRealPipelining.resumeReading should be called when the response has been sent
    //
    // PITFALL:
    // If this line is after the line "ctx.fireChannelRead(env)" (right below),
    // this order may happen: resumeReading -> pauseReading
    //
    // We want: pauseReading -> resumeReading
    NoRealPipelining.pauseReading(ctx.channel)

    ctx.fireChannelRead(env)

    // Reset for the next request on this same connection (e.g. keep alive)
    env               = null
    bodyBytesReceived = 0
  }

  private def isAPPLICATION_JSON(request: HttpRequest): Boolean = {
    val requestContentType = HttpHeaders.getHeader(request, HttpHeaders.Names.CONTENT_TYPE)
    if (requestContentType == null) return false

    val requestContentTypeLowerCase = requestContentType.toLowerCase
    requestContentTypeLowerCase.startsWith("application/json")
  }

  /**
   * Only parses one level.
   * Should use requestContentJValue directly for more advanced uses.
   */
  private def parseTextParamsFromJson(env: HandlerEnv) {
    env.requestContentJValue match {
      case JObject(fields) =>
        fields.foreach {
          case JField(name, JArray(values)) =>
            values.foreach { value =>
              putOrAppendString(env.bodyTextParams, name, jValue2String(value))
            }

          case JField(name, value) =>
            putOrAppendString(env.bodyTextParams, name, jValue2String(value))
        }

      case _ => // Nothing to do
    }
  }

  private def jValue2String(value: JValue) = value match {
    case JNull | JNothing => "null"
    case JString(value)   => value
    case JInt(value)      => value.toString
    case JDouble(value)   => value.toString
    case JDecimal(value)  => value.toString
    case JBool(value)     => value.toString
    case value            => JsonMethods.compact(value)
  }
}
