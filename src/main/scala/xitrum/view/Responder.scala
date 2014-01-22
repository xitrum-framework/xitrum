package xitrum.view

import java.io.File
import scala.xml.{Node, NodeSeq, Xhtml}
import scala.util.control.NonFatal

import io.netty.buffer.{ByteBuf, Unpooled}
import io.netty.channel.{ChannelFuture, ChannelFutureListener}
import io.netty.handler.codec.http.{
  DefaultHttpContent, DefaultLastHttpContent,
  HttpHeaders, HttpResponseStatus, HttpVersion, LastHttpContent
}
import io.netty.util.CharsetUtil
import HttpHeaders.Names.{CONTENT_TYPE, CONTENT_LENGTH, TRANSFER_ENCODING}
import HttpHeaders.Values.{CHUNKED, NO_CACHE}

import xitrum.{Action, Config}
import xitrum.etag.NotModified
import xitrum.handler.inbound.NoPipelining
import xitrum.handler.outbound.{XSendFile, XSendResource}
import xitrum.util.Json

/**
 * When responding text, charset is automatically set, as advised by Google:
 * http://code.google.com/speed/page-speed/docs/rendering.html#SpecifyCharsetEarly
 */
trait Responder extends Js with Flash with GetActionClassDefaultsToCurrentAction {
  this: Action =>

  //----------------------------------------------------------------------------

  private var nonChunkedResponseOrFirstChunkSent = false
  private var doneResponding                     = false

  def isDoneResponding = doneResponding

  /**
   * Called when the response or the last chunk (in case of chunked response)
   * has been sent to the client.
   */
  def onDoneResponding() {}

  def respond(): ChannelFuture = {
    if (nonChunkedResponseOrFirstChunkSent) {
      printDoubleResponseErrorStackTrace()
    } else {
      NoPipelining.setResponseHeaderForKeepAliveRequest(request, response)
      setCookieAndSessionIfTouchedOnRespond()
      val future = channel.writeAndFlush(handlerEnv)

      // Do not handle keep alive:
      // * If XSendFile or XSendResource is used because it is handled by them
      //   in their own way
      // * If the response is chunked because it will be handled by respondLastChunk
      if (!XSendFile.isHeaderSet(response) &&
          !XSendResource.isHeaderSet(response) &&
          !HttpHeaders.isTransferEncodingChunked(response)) {
        NoPipelining.if_keepAliveRequest_then_resumeReading_else_closeOnComplete(request, channel, future)
      }

      nonChunkedResponseOrFirstChunkSent = true
      if (!HttpHeaders.isTransferEncodingChunked(response)) {
        doneResponding = true
        onDoneResponding()
      }

      future
    }
  }

  //----------------------------------------------------------------------------

  /**
   * To respond chunks (http://en.wikipedia.org/wiki/Chunked_transfer_encoding):
   * 1. Call setChunked() to mark that the response will be chunked
   * 2. Call respondXXX as normal, but as many times as you want
   * 3. Lastly, call respondLastChunk()
   *
   * If Content-Type header is not set, it is set to "application/octet-stream".
   */
  def setChunked() {
    HttpHeaders.setTransferEncodingChunked(response)

    // From now on, the header is a mark telling the response is chunked.
    // It should not be removed from the response.
  }

  /** If Content-Type header is not set, it is set to "application/octet-stream" */
  private def respondHeadersOnlyForFirstChunk() {
    if (nonChunkedResponseOrFirstChunkSent) return

    if (!response.headers.contains(CONTENT_TYPE))
      HttpHeaders.setHeader(response, CONTENT_TYPE, "application/octet-stream")

    // There should be no CONTENT_LENGTH header
    HttpHeaders.removeHeader(response, CONTENT_LENGTH)

    setNoClientCache()

    // Env2Response will respond only only headers
    respond()
  }

  /** See setChunked. */
  def respondLastChunk(trailingHeaders: HttpHeaders = HttpHeaders.EMPTY_HEADERS): ChannelFuture = {
    if (!HttpHeaders.isTransferEncodingChunked(response)) {
      printDoubleResponseErrorStackTrace()
    } else {
      val trailer =
        if (trailingHeaders.isEmpty) {
          LastHttpContent.EMPTY_LAST_CONTENT
        } else {
          val ret = new DefaultLastHttpContent
          ret.trailingHeaders.set(trailingHeaders)
          ret
        }
      val future = channel.writeAndFlush(trailer)
      NoPipelining.if_keepAliveRequest_then_resumeReading_else_closeOnComplete(request, channel, future)

      // responded should be true here. Set chunk mode to false so that double
      // response error will be raised if the app try to respond more.
      HttpHeaders.removeTransferEncodingChunked(response)

      doneResponding = true
      onDoneResponding()

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

    if (!nonChunkedResponseOrFirstChunkSent && !response.headers.contains(CONTENT_TYPE)) {
      // Set content type
      if (fallbackContentType != null) {
        // https://developers.google.com/speed/docs/best-practices/rendering#SpecifyCharsetEarly
        val withCharset =
          if (fallbackContentType.toLowerCase.contains("charset"))
            fallbackContentType
          else
            fallbackContentType + "; charset=" + Config.xitrum.request.charset

        HttpHeaders.setHeader(response, CONTENT_TYPE, withCharset)
      } else {
        if (textIsXml)
          HttpHeaders.setHeader(response, CONTENT_TYPE, "application/xml; charset=" + Config.xitrum.request.charset)
        else
          HttpHeaders.setHeader(response, CONTENT_TYPE, "text/plain; charset=" + Config.xitrum.request.charset)
      }
    }

    val bytes = respondedText.getBytes(Config.xitrum.request.charset)
    val cb    = Unpooled.wrappedBuffer(bytes)
    if (HttpHeaders.isTransferEncodingChunked(response)) {
      respondHeadersOnlyForFirstChunk()
      channel.writeAndFlush(new DefaultHttpContent(cb))
    } else {
      // Content length is number of bytes, not characters!
      HttpHeaders.setContentLength(response, bytes.length)
      response.content(cb)
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
  def respondJson(ref: AnyRef): ChannelFuture = {
    val json = Json.generate(ref)
    respondText(json, "application/json")
  }

  /**
   * Converts the given Scala object to JSON object, wraps it with the given
   * JavaScript function name, and responds. If you already have a JSON text,
   * thus no conversion is needed, use respondJsonPText.
   *
   * Content-Type header is set to "application/javascript".
   */
  def respondJsonP(ref: AnyRef, function: String): ChannelFuture = {
    val json = Json.generate(ref)
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
  def respondView(customLayout: () => Any, location: Class[_ <: Action], options: Map[String, Any]): ChannelFuture = {
    val string = renderView(customLayout, location, options)
    respondText(string, "text/html")
  }

  def respondView[T <: Action : Manifest](customLayout: () => Any, options: Map[String, Any]): ChannelFuture = {
    respondView(customLayout, getActionClass[T], options)
  }

  def respondView[T <: Action : Manifest](customLayout: () => Any): ChannelFuture = {
    respondView(customLayout, getActionClass[T], Map.empty)
  }

  def respondView[T <: Action : Manifest](options: Map[String, Any]): ChannelFuture = {
    respondView(layout _, getActionClass[T], options)
  }

  def respondView[T <: Action : Manifest](): ChannelFuture = {
    respondView(layout _, getActionClass[T], Map.empty)
  }
  //----------------------------------------------------------------------------

  /** Content-Type header is set to "text/html". */
  def respondViewNoLayout(location: Class[_ <: Action], options: Map[String, Any]): ChannelFuture = {
    val string = renderViewNoLayout(location, options)
    respondText(string, "text/html")
  }

  def respondViewNoLayout[T <: Action : Manifest](options: Map[String, Any]): ChannelFuture = {
    respondViewNoLayout(getActionClass[T], options)
  }

  def respondViewNoLayout[T <: Action : Manifest](): ChannelFuture = {
    respondViewNoLayout(getActionClass[T], Map.empty)
  }

  //----------------------------------------------------------------------------

  /** Content-Type header is set to "text/html". */
  def respondInlineView(inlineView: Any): ChannelFuture = {
    val string = renderInlineView(inlineView)
    respondText(string, "text/html")
  }

  //----------------------------------------------------------------------------

  /** If Content-Type header is not set, it is set to "application/octet-stream". */
  def respondBinary(bytes: Array[Byte]): ChannelFuture = {
    respondBinary(Unpooled.wrappedBuffer(bytes))
  }

  /**
   * If Content-Type header is not set, it is set to "application/octet-stream".
   *
   * @param byteBuf Will be released
   */
  def respondBinary(byteBuf: ByteBuf): ChannelFuture = {
    if (HttpHeaders.isTransferEncodingChunked(response)) {
      respondHeadersOnlyForFirstChunk()
      channel.writeAndFlush(new DefaultHttpContent(byteBuf))
    } else {
      if (!response.headers.contains(CONTENT_TYPE))
        HttpHeaders.setHeader(response, CONTENT_TYPE, "application/octet-stream")
      HttpHeaders.setContentLength(response, byteBuf.readableBytes)
      response.content(byteBuf)
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

  /**
   * To respond event source, call this method as many time as you want.
   * Event Source response is a special kind of chunked response, data must be UTF-8.
   * See:
   * - http://sockjs.github.com/sockjs-protocol/sockjs-protocol-0.3.3.html#section-94
   * - http://dev.w3.org/html5/eventsource/
   *
   * No need to call setChunked() before calling this method.
   */
  def respondEventSource(data: Any, event: String = "message"): ChannelFuture = {
    if (!nonChunkedResponseOrFirstChunkSent) {
      HttpHeaders.setTransferEncodingChunked(response)
      HttpHeaders.setHeader(response, CONTENT_TYPE, "text/event-stream; charset=UTF-8")
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
      case NonFatal(e) =>
        log.warn("Double response! This double response is ignored.", e)
    }
    null  // This may cause NPE on double response if the ChannelFuture result is used
  }

}
