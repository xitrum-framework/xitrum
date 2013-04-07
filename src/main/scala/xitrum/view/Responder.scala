package xitrum.view

import java.io.File
import scala.xml.{Node, NodeSeq, Xhtml}

import org.jboss.netty.buffer.{ChannelBuffer, ChannelBuffers}
import org.jboss.netty.channel.ChannelFuture
import org.jboss.netty.handler.codec.http.{DefaultHttpChunk, HttpChunk, HttpHeaders, HttpResponseStatus, HttpVersion}
import org.jboss.netty.handler.codec.http.websocketx.TextWebSocketFrame
import org.jboss.netty.util.CharsetUtil
import HttpHeaders.Names.{CONTENT_TYPE, CONTENT_LENGTH, TRANSFER_ENCODING}
import HttpHeaders.Values.{CHUNKED, NO_CACHE}

import xitrum.{Action, Config}
import xitrum.etag.NotModified
import xitrum.handler.up.NoPipelining
import xitrum.handler.down.{XSendFile, XSendResource}
import xitrum.routing.Routes
import xitrum.util.Json

/**
 * When responding text, charset is automatically set, as advised by Google:
 * http://code.google.com/speed/page-speed/docs/rendering.html#SpecifyCharsetEarly
 */
trait Responder extends JS with Flash with Knockout {
  this: Action =>

  //----------------------------------------------------------------------------

  private var responded = false

  def isResponded = responded

  def respond(): ChannelFuture = {
    if (responded) {
      printDoubleResponseErrorStackTrace()
    } else {
      NoPipelining.setResponseHeaderForKeepAliveRequest(request, response)
      setCookieAndSessionIfTouchedOnRespond()
      val future = channel.write(handlerEnv)

      // Do not handle keep alive:
      // * If XSendFile or XSendResource is used because it is handled by them
      //   in their own way
      // * If the response is chunked because it will be handled by respondLastChunk
      if (!XSendFile.isHeaderSet(response) &&
          !XSendResource.isHeaderSet(response) &&
          !response.isChunked) {
        NoPipelining.if_keepAliveRequest_then_resumeReading_else_closeOnComplete(request, channel, future)
      }

      responded = true
      Config.actorSystem.stop(self)

      future
    }
  }

  //----------------------------------------------------------------------------

  /** If Content-Type header is not set, it is set to "application/octet-stream" */
  private def respondHeadersForFirstChunk() {
    if (responded) return

    if (!response.containsHeader(CONTENT_TYPE))
      response.setHeader(CONTENT_TYPE, "application/octet-stream")

    // There should be no CONTENT_LENGTH header
    response.removeHeader(CONTENT_LENGTH)

    setNoClientCache()

    // TRANSFER_ENCODING header is automatically set by Netty when it send the
    // real response. We don't need to manually set it here.
    // However, this header is not allowed in HTTP/1.0:
    // http://sockjs.github.com/sockjs-protocol/sockjs-protocol-0.3.3.html#section-165
    if (request.getProtocolVersion.compareTo(HttpVersion.HTTP_1_0) == 0) {
      response.setChunked(false)
      respond()
      response.setChunked(true)
    } else {
      respond()
    }
  }

  /**
   * To respond chunks (http://en.wikipedia.org/wiki/Chunked_transfer_encoding):
   * 1. Call response.setChunked(true)
   * 2. Call respondXXX as many times as you want
   * 3. Lastly, call respondLastChunk()
   *
   * Headers are only sent on the first respondXXX call.
   */
  def respondLastChunk(): ChannelFuture = {
    if (!response.isChunked()) {
      printDoubleResponseErrorStackTrace()
    } else {
      val future = channel.write(HttpChunk.LAST_CHUNK)
      NoPipelining.if_keepAliveRequest_then_resumeReading_else_closeOnComplete(request, channel, future)

      // responded should be true here. Set chunk mode to false so that double
      // response error will be raised if the app try to respond more.
      response.setChunked(false)

      future
    }
  }

  //----------------------------------------------------------------------------

  /**
   * @param fallbackContentType Only used if Content-Type header has not been set.
   * If not given and Content-Type header is not set, it is set to
   * "application/xml" if text param is Node or NodeSeq, otherwise it is
   * set to "text/plain".
   *
   * @param convertXmlToXhtml <br />.toString by default returns <br></br> which
   * is rendered as 2 <br /> tags on some browsers! Set to false if you really
   * want XML, not XHTML. See http://www.scala-lang.org/node/492 and
   * http://www.ne.jp/asahi/hishidama/home/tech/scala/xml.html
   */
  def respondText(text: Any, fallbackContentType: String = null, convertXmlToXhtml: Boolean = true): ChannelFuture = {
    val textIsXml = text.isInstanceOf[Node] || text.isInstanceOf[NodeSeq]

    val respondedText =
      if (textIsXml && convertXmlToXhtml) {
        if (text.isInstanceOf[Node])
          Xhtml.toXhtml(text.asInstanceOf[Node])
        else
          Xhtml.toXhtml(text.asInstanceOf[NodeSeq])
      } else {
        text.toString
      }

    if (!responded && !response.containsHeader(CONTENT_TYPE)) {
      // Set content type
      if (fallbackContentType != null) {
        // https://developers.google.com/speed/docs/best-practices/rendering#SpecifyCharsetEarly
        val withCharset =
          if (fallbackContentType.toLowerCase.contains("charset"))
            fallbackContentType
          else
            fallbackContentType + "; charset=" + Config.xitrum.request.charset

        response.setHeader(CONTENT_TYPE, withCharset)
      } else {
        if (textIsXml)
          response.setHeader(CONTENT_TYPE, "application/xml; charset=" + Config.xitrum.request.charset)
        else
          response.setHeader(CONTENT_TYPE, "text/plain; charset=" + Config.xitrum.request.charset)
      }
    }

    val cb = ChannelBuffers.copiedBuffer(respondedText, Config.requestCharset)
    if (response.isChunked) {
      respondHeadersForFirstChunk()
      channel.write(new DefaultHttpChunk(cb))
    } else {
      // Content length is number of bytes, not characters!
      HttpHeaders.setContentLength(response, cb.readableBytes)
      response.setContent(cb)
      respond()
    }
  }

  //----------------------------------------------------------------------------

  /** Content-Type header is set to "application/xml". */
  def respondXml(any: Any): ChannelFuture = {
    respondText(any, "application/xml", false)
  }

  /** Content-Type header is set to "text/html". */
  def respondHtml(any: Any): ChannelFuture = {
    respondText(any, "text/html")
  }

  /** Content-Type header is set to "application/javascript". */
  def respondJs(any: Any): ChannelFuture = {
    respondText(any, "application/javascript")
  }

  /** Content-Type header is set to "application/json". */
  def respondJsonText(any: Any): ChannelFuture = {
    respondText(any, "application/json")
  }

  /**
   * Converts the given Scala object to JSON object, and responds it.
   * If you just want to respond a text with "application/json" as content type,
   * use respondJsonText(text).
   *
   * Content-Type header is set to "application/json".
   * "text/json" would make the browser download instead of displaying the content.
   * It makes debugging a pain.
   */
  def respondJson(obj: AnyRef): ChannelFuture = {
    val json = Json.generate(obj)
    respondText(json, "application/json")
  }

  /**
   * Converts the given Scala object to JSON object, wraps it with the given
   * JavaScript function name, and responds. If you already have a JSON text,
   * thus no conversion is needed, use respondJsonPText.
   *
   * Content-Type header is set to "application/javascript".
   */
  def respondJsonP(obj: AnyRef, function: String): ChannelFuture = {
    val json = Json.generate(obj)
    val text = function + "(" + json + ");\r\n"
    respondJs(text)
  }

  /**
   * Wraps the text with the given JavaScript function name, and responds.
   *
   * Content-Type header is set to "application/javascript".
   */
  def respondJsonPText(text: Any, function: String): ChannelFuture = {
    respondJs(function + "(" + text + ");\r\n")
  }

  //----------------------------------------------------------------------------

  /**
   * @param options specific to the configured template engine
   */
  def respondView(customLayout: () => Any = layout _, actionClass: Class[_ <: Action] = getClass, options: Map[String, Any] = Map()): ChannelFuture = {
    val string = renderView(customLayout, actionClass, options)
    respondText(string, "text/html")
  }
/*
  def respondView(customLayout: () => Any, options: Map[String, Any]): ChannelFuture =
    respondView(currentAction.getClass, customLayout, options)

  def respondView(customLayout: () => Any): ChannelFuture =
    respondView(currentAction.getClass, customLayout, Map[String, Any]())

  /**
   * Same as respondView(action, customLayout, options),
   * where customLayout is the controller's layout method.
   */
  def respondView(action: Class[_ <: Action], options: Map[String, Any]): ChannelFuture =
    respondView(action, layout _, options)

  def respondView(action: Class[_ <: Action]): ChannelFuture =
    respondView(action, layout _, Map[String, Any]())

  /**
   * Same as respondView(action, customLayout, options),
   * where action is currentAction and customLayout is the controller's layout method.
   */
  def respondView(options: Map[String, Any]): ChannelFuture =
    respondView(currentAction.getClass, layout _, options)

  def respondView(): ChannelFuture =
    respondView(currentAction.getClass, layout _, Map[String, Any]())
*/
  //----------------------------------------------------------------------------

  /** Content-Type header is set to "text/html" */
  def respondInlineView(inlineView: Any): ChannelFuture = {
    val string = renderInlineView(inlineView)
    respondText(string, "text/html")
  }

  /** Content-Type header is set to "text/html" */
  def respondViewNoLayout(actionClass: Class[_ <: Action] = getClass, options: Map[String, Any] = Map()): ChannelFuture = {
    val string = renderViewNoLayout(actionClass, options)
    respondText(string, "text/html")
  }
/*
  def respondViewNoLayout(controllerClass: Class[_]): ChannelFuture =
    respondViewNoLayout(controllerClass, Map[String, Any]())
*/
  //----------------------------------------------------------------------------

  /** If Content-Type header is not set, it is set to "application/octet-stream" */
  def respondBinary(bytes: Array[Byte]): ChannelFuture = {
    respondBinary(ChannelBuffers.wrappedBuffer(bytes))
  }

  /** If Content-Type header is not set, it is set to "application/octet-stream" */
  def respondBinary(channelBuffer: ChannelBuffer): ChannelFuture = {
    if (response.isChunked) {
      respondHeadersForFirstChunk()
      channel.write(new DefaultHttpChunk(channelBuffer))
    } else {
      if (!response.containsHeader(CONTENT_TYPE))
        response.setHeader(CONTENT_TYPE, "application/octet-stream")
      HttpHeaders.setContentLength(response, channelBuffer.readableBytes)
      response.setContent(channelBuffer)
      respond()
    }
  }

  /**
   * Sends a file using X-SendFile.
   * If Content-Type header is not set, it is guessed from the file name.
   *
   * @param path absolute or relative to the current working directory
   *
   * In some cases, the current working directory is not always the root directory
   * of the project (https://github.com/ngocdaothanh/xitrum/issues/47), you may
   * need to use xitrum.Config.root to calculate the correct absolute path from
   * a relative path.
   *
   * To sanitize the path, use xitrum.util.PathSanitizer.
   */
  def respondFile(path: String): ChannelFuture = {
    XSendFile.setHeader(response, path, true)
    respond()
  }

  /**
   * Sends a file from public directory in one of the entry (may be a JAR file)
   * in classpath.
   * If Content-Type header is not set, it is guessed from the file name.
   *
   * @param path Relative to an entry in classpath, without leading "/"
   */
  def respondResource(path: String): ChannelFuture = {
    XSendResource.setHeader(response, path, true)
    respond()
  }

  //----------------------------------------------------------------------------

  def respondWebSocket(text: Any): ChannelFuture = {
    channel.write(new TextWebSocketFrame(text.toString))
  }

  def respondWebSocket(channelBuffer: ChannelBuffer): ChannelFuture = {
    channel.write(new TextWebSocketFrame(channelBuffer))
  }

  //----------------------------------------------------------------------------

  /**
   * To respond event source, call this method as many time as you want.
   * Event Source response is a special kind of chunked response.
   * Data must be Must be  UTF-8.
   * See:
   * - http://sockjs.github.com/sockjs-protocol/sockjs-protocol-0.3.3.html#section-94
   * - http://dev.w3.org/html5/eventsource/
   */
  def respondEventSource(data: Any, event: String = "message"): ChannelFuture = {
    if (!responded) {
      response.setHeader(CONTENT_TYPE, "text/event-stream; charset=UTF-8")
      response.setChunked(true)
      respondText("\r\n")  // Send a new line prelude, due to a bug in Opera
    }
    respondText(renderEventSource(data, event))
  }

  //----------------------------------------------------------------------------

  def respondDefault404Page(): ChannelFuture = {
    if (isAjax) {
      response.setStatus(HttpResponseStatus.NOT_FOUND)
      jsRespond("alert(\"" + jsEscape("Not Found") + "\")")
    } else {
      XSendFile.set404Page(response, true)
      respond()
    }
  }

  def respondDefault500Page(): ChannelFuture = {
    if (isAjax) {
      response.setStatus(HttpResponseStatus.INTERNAL_SERVER_ERROR)
      jsRespond("alert(\"" + jsEscape("Internal Server Error") + "\")")
    } else {
      XSendFile.set500Page(response, true)
      respond()
    }
  }

  //----------------------------------------------------------------------------

  def setClientCacheAggressively() {
    NotModified.setClientCacheAggressively(response)
  }

  def setNoClientCache() {
    NotModified.setNoClientCache(response)
  }

  //----------------------------------------------------------------------------

  /**
   * Prints the stack trace so that application developers know where to fix
   * the double response error.
   */
  private def printDoubleResponseErrorStackTrace(): ChannelFuture = {
    try {
      throw new IllegalStateException("Double response")
    } catch {
      case scala.util.control.NonFatal(e) =>
        logger.warn("Double response! This double response is ignored.", e)
    }
    null  // This may cause NPE on double response if the ChannelFuture result is used
  }
}
