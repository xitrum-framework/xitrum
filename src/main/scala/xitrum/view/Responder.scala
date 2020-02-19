package xitrum.view

import scala.xml.{Node, NodeSeq, Xhtml}

import io.netty.buffer.{ByteBuf, Unpooled}
import io.netty.channel.ChannelFuture
import io.netty.handler.codec.http.{
  DefaultHttpContent, DefaultLastHttpContent,
  EmptyHttpHeaders, HttpHeaderNames, HttpHeaders,
  HttpResponseStatus, HttpUtil, LastHttpContent
}
import HttpHeaderNames.{CONTENT_TYPE, CONTENT_LENGTH}

import xitrum.{Action, Config}
import xitrum.etag.NotModified
import xitrum.handler.NoRealPipelining
import xitrum.handler.outbound.{XSendFile, XSendResource}
import xitrum.util.{ByteBufUtil, SeriDeseri}

/**
 * When responding text, charset is automatically set, as advised by Google:
 * http://code.google.com/speed/page-speed/docs/rendering.html#SpecifyCharsetEarly
 */
trait Responder {
  this: Action =>

  //----------------------------------------------------------------------------

  private var nonChunkedResponseOrFirstChunkSent = false
  private var doneResponding                     = false

  def isDoneResponding: Boolean = doneResponding

  /**
   * Called when the response or the last chunk (in case of chunked response)
   * has been sent to the client.
   */
  def onDoneResponding(): Unit = {}

  def respond(): ChannelFuture = {
    // For chunked response, this method is only called to respond the 1st chunk,
    // next chunks are responded directly by respondXXX

    if (nonChunkedResponseOrFirstChunkSent) throwDoubleResponseError()

    setCookieAndSessionIfTouchedOnRespond()
    val future = channel.writeAndFlush(handlerEnv)

    // Do not handle keep alive:
    // * If XSendFile or XSendResource is used, because they will handle keep alive in their own way
    // * If the response is chunked, because respondLastChunk will be handle keep alive
    if (!XSendFile.isHeaderSet(response) &&
        !XSendResource.isHeaderSet(response) &&
        !HttpUtil.isTransferEncodingChunked(response)) {
      NoRealPipelining.if_keepAliveRequest_then_resumeReading_else_closeOnComplete(request, channel, future)
    }

    nonChunkedResponseOrFirstChunkSent = true
    if (!HttpUtil.isTransferEncodingChunked(response)) {
      doneResponding = true
      onDoneResponding()
    }

    future
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
  def setChunked(): Unit = {
    HttpUtil.setTransferEncodingChunked(response, true)

    // From now on, the header is a mark telling the response is chunked.
    // It should not be removed from the response.
  }

  /** See setChunked. */
  def respondLastChunk(trailingHeaders: HttpHeaders = EmptyHttpHeaders.INSTANCE): ChannelFuture = {
    if (doneResponding) throwDoubleResponseError()

    val trailer = if (trailingHeaders.isEmpty) {
      LastHttpContent.EMPTY_LAST_CONTENT
    } else {
      val ret = new DefaultLastHttpContent
      ret.trailingHeaders.set(trailingHeaders)
      ret
    }
    val future = channel.writeAndFlush(trailer)
    NoRealPipelining.if_keepAliveRequest_then_resumeReading_else_closeOnComplete(request, channel, future)

    doneResponding = true
    onDoneResponding()

    future
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
    if (doneResponding) throwDoubleResponseError(Some(text))

    val instanceOfNode = text.isInstanceOf[Node]
    val textIsXml      = instanceOfNode || text.isInstanceOf[NodeSeq]

    val respondedText =
      if (textIsXml && convertXmlToXhtml) {
        if (instanceOfNode)
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

        response.headers.set(CONTENT_TYPE, withCharset)
      } else {
        if (textIsXml)
          response.headers.set(CONTENT_TYPE, "application/xml; charset=" + Config.xitrum.request.charset)
        else
          response.headers.set(CONTENT_TYPE, "text/plain; charset=" + Config.xitrum.request.charset)
      }
    }

    val buf = Unpooled.copiedBuffer(respondedText, Config.xitrum.request.charset)
    if (HttpUtil.isTransferEncodingChunked(response)) {
      respondHeadersOnlyForFirstChunk()
      channel.writeAndFlush(new DefaultHttpContent(buf))
    } else {
      ByteBufUtil.writeComposite(response.content, buf)
      respond()
    }
  }

  //----------------------------------------------------------------------------

  /** Content-Type header is set to "application/xml". */
  def respondXml(any: Any): ChannelFuture = {
    respondText(any, "application/xml", convertXmlToXhtml = false)
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
    val json = SeriDeseri.toJson(ref)
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
    val json = SeriDeseri.toJson(ref)
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
    if (HttpUtil.isTransferEncodingChunked(response)) {
      respondHeadersOnlyForFirstChunk()
      channel.writeAndFlush(new DefaultHttpContent(byteBuf))
    } else {
      if (!response.headers.contains(CONTENT_TYPE))
        response.headers.set(CONTENT_TYPE, "application/octet-stream")
      ByteBufUtil.writeComposite(response.content, byteBuf)
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
   * of the project (https://github.com/xitrum-framework/xitrum/issues/47), you may
   * need to use xitrum.Config.root to calculate the correct absolute path from
   * a relative path.
   *
   * To sanitize the path, use xitrum.util.PathSanitizer.
   */
  def respondFile(path: String): ChannelFuture = {
    XSendFile.setHeader(response, path, fromAction = true)
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
    XSendResource.setHeader(response, path, fromController = true)
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
      HttpUtil.setTransferEncodingChunked(response, true)
      response.headers.set(CONTENT_TYPE, "text/event-stream; charset=UTF-8")
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
      XSendFile.set404Page(response, fromController = true)
      respond()
    }
  }

  def respondDefault500Page(): ChannelFuture = {
    if (isAjax) {
      response.setStatus(HttpResponseStatus.INTERNAL_SERVER_ERROR)
      jsRespond("alert(\"" + jsEscape("Internal Server Error") + "\")")
    } else {
      XSendFile.set500Page(response, fromController = true)
      respond()
    }
  }

  //----------------------------------------------------------------------------

  def respond404Page(): Unit = {
    if (isAjax) {
      response.setStatus(HttpResponseStatus.NOT_FOUND)
      jsRespond("alert(\"" + jsEscape("Not Found") + "\")")
    } else {
      Config.routes.error404 match {
        case None =>
          XSendFile.set404Page(response, fromController = true)
          respond()

        case Some(error404) =>
          response.setStatus(HttpResponseStatus.NOT_FOUND)
          forwardTo(error404)
      }
    }
  }

  def respond500Page(): Unit = {
    if (isAjax) {
      response.setStatus(HttpResponseStatus.INTERNAL_SERVER_ERROR)
      jsRespond("alert(\"" + jsEscape("Internal Server Error") + "\")")
    } else {
      Config.routes.error500 match {
        case None =>
          XSendFile.set500Page(response, fromController = true)
          respond()

        case Some(error500) =>
          response.setStatus(HttpResponseStatus.INTERNAL_SERVER_ERROR)
          forwardTo(error500)
      }
    }
  }

  //----------------------------------------------------------------------------

  /**
   * Tells the browser to cache static files for a long time.
   * This works well even when this is a cluster of web servers behind a load balancer
   * because the URL created by urlForResource is in the form: resource?etag
   *
   * Don't worry that browsers do not pick up new files after you modified them,
   * see the doc about static files.
   *
   * Google recommends 1 year:
   * http://code.google.com/speed/page-speed/docs/caching.html
   *
   * Both Max-age and Expires header are set because IEs use Expires, not max-age:
   * http://mrcoles.com/blog/cookies-max-age-vs-expires/
   */
  def setClientCacheAggressively(): Unit = {
    NotModified.setClientCacheAggressively(response)
  }

  /**
   * Prevents client cache.
   * Note that "pragma: no-cache" is linked to requests, not responses:
   * http://palizine.plynt.com/issues/2008Jul/cache-control-attributes/
   */
  def setNoClientCache(): Unit = {
    NotModified.setNoClientCache(response)
  }

  //----------------------------------------------------------------------------

  private def throwDoubleResponseError(texto: Option[Any] = None): Unit = {
    texto match {
      case None =>
        throw new IllegalStateException("Double response error. See stack trace to know where to fix the error.")

      case Some(text) =>
        throw new IllegalStateException(s"Double response error. See stack trace to know where to fix the error. You're trying to respond: $text\n")
    }
  }

  /** If Content-Type header is not set, it is set to "application/octet-stream" */
  private def respondHeadersOnlyForFirstChunk(): Unit = {
    // doneResponding is set to true by respondLastChunk
    if (doneResponding) throwDoubleResponseError()

    if (nonChunkedResponseOrFirstChunkSent) return

    if (!response.headers.contains(CONTENT_TYPE))
      response.headers.set(CONTENT_TYPE, "application/octet-stream")

    // There should be no CONTENT_LENGTH header
    response.headers.remove(CONTENT_LENGTH)

    setNoClientCache()

    // Env2Response will respond only only headers
    respond()
  }
}
