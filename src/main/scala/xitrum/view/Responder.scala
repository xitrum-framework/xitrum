package xitrum.view

import java.io.File
import scala.xml.{Node, NodeSeq, Xhtml}

import io.netty.buffer.ChannelBuffers
import io.netty.handler.codec.http.{DefaultHttpChunk, HttpChunk, HttpHeaders}
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame
import HttpHeaders.Names.{CONTENT_TYPE, CONTENT_LENGTH, TRANSFER_ENCODING}
import HttpHeaders.Values.CHUNKED

import com.codahale.jerkson.Json

import xitrum.{Controller, Config}
import xitrum.routing.Route
import xitrum.handler.down.{XSendFile, XSendResource}

/**
 * When responding text, charset is automatically set, as advised by Google:
 * http://code.google.com/speed/page-speed/docs/rendering.html#SpecifyCharsetEarly
 */
trait Responder extends JS with Flash with Knockout with I18n {
  this: Controller =>

  private def writeHeaderOnFirstChunk {
    if (!isResponded) {
      response.removeHeader(CONTENT_LENGTH)
      response.setHeader(TRANSFER_ENCODING, CHUNKED)
      respond
    }
  }

  /**
   * To respond chunks (http://en.wikipedia.org/wiki/Chunked_transfer_encoding):
   * 1. Call response.setChunked(true)
   * 2. Call respondXXX as many times as you want
   * 3. Lastly, call respondLastChunk
   *
   * Headers are only sent on the first respondXXX call.
   */
  def respondLastChunk {
    channel.write(HttpChunk.LAST_CHUNK)
  }

  //----------------------------------------------------------------------------

  /**
   * If contentType param is not given and Content-Type header is not set, it is
   * set to "application/xml" if text param is Node or NodeSeq, otherwise it is
   * set to "text/plain".
   */
  def respondText(text: Any, contentType: String = null): String = {
    val textIsXml = text.isInstanceOf[Node] || text.isInstanceOf[NodeSeq]

    // <br />.toString will create <br></br> which responds as 2 <br /> on some browsers!
    // http://www.scala-lang.org/node/492
    // http://www.ne.jp/asahi/hishidama/home/tech/scala/xml.html
    val ret =
      if (textIsXml) {
        if (text.isInstanceOf[Node])
          Xhtml.toXhtml(text.asInstanceOf[Node])
        else
          Xhtml.toXhtml(text.asInstanceOf[NodeSeq])
      } else {
        text.toString
      }

    if (!isResponded) {
      // Set content type automatically
      if (contentType != null)
        response.setHeader(CONTENT_TYPE, contentType)
      else if (!response.containsHeader(CONTENT_TYPE)) {
        if (textIsXml)
          response.setHeader(CONTENT_TYPE, "application/xml; charset=" + Config.config.request.charset)
        else
          response.setHeader(CONTENT_TYPE, "text/plain; charset=" + Config.config.request.charset)
      }
    }

    val cb = ChannelBuffers.copiedBuffer(ret, Config.requestCharset)
    if (response.isChunked) {
      writeHeaderOnFirstChunk
      val chunk = new DefaultHttpChunk(cb)
      channel.write(chunk)
    } else {
      // Content length is number of bytes, not characters!
      HttpHeaders.setContentLength(response, cb.readableBytes)
      response.setContent(cb)
      respond
    }

    ret
  }

  //----------------------------------------------------------------------------

  /**
   * Content-Type header is set to "application/json".
   * With text/json browser downloads it instead of displaying it,
   * which makes debugging a pain.
   */
  def respondJson(any: Any) {
    val json = Json.generate(any)
    respondText(json, "application/json; charset=" + Config.config.request.charset)
  }

  //----------------------------------------------------------------------------

  var renderedView: Any = null

  def layout = renderedView

  def respondInlineView(view: Any) {
    respondInlineView(view, layout _)
  }

  /** Content-Type header is set to "text/html" */
  def respondInlineView(view: Any, customLayout: () => Any) {
    renderedView = view
    val respondedLayout = customLayout.apply()
    if (respondedLayout == null)
      respondText(renderedView, "text/html; charset=" + Config.config.request.charset)
    else
      respondText(respondedLayout, "text/html; charset=" + Config.config.request.charset)
  }

  //----------------------------------------------------------------------------

  def renderScalate(path: String) = Scalate.renderFile(this, path)

  def renderScalate(controllerClass: Class[_], templateType: String): String = {
    val path = "src/main/scalate/" + controllerClass.getName.replace(".", "/") + "." + templateType
    renderScalate(path)
  }

  def renderScalate(controllerClass: Class[_]): String = {
    renderScalate(controllerClass, Config.config.scalate)
  }

  def respondView(templateType: String) {
    respondView(currentRoute, templateType)
  }

  def respondView() {
    respondView(currentRoute, Config.config.scalate)
  }

  def respondView(route: Route, templateType: String) {
    respondView(route, layout _, templateType)
  }

  def respondView(route: Route) {
    respondView(route, layout _, Config.config.scalate)
  }

  def respondView(route: Route, customLayout: () => Any, templateType: String) {
    val nonNullRouteMethod = nonNullRouteMethodFromRoute(route)
    val controllerClass    = nonNullRouteMethod.getDeclaringClass
    val routeName          = nonNullRouteMethod.getName
    val path = "src/main/scalate/" + controllerClass.getName.replace(".", "/") + "/" + routeName + "." + templateType

    renderedView = renderScalate(path)
    val respondedLayout = customLayout.apply
    if (respondedLayout == null)
      respondText(renderedView, "text/html; charset=" + Config.config.request.charset)
    else
      respondText(respondedLayout, "text/html; charset=" + Config.config.request.charset)
  }

  def respondView(route: Route, customLayout: () => Any) {
    respondView(route, customLayout, Config.config.scalate)
  }

  //----------------------------------------------------------------------------

  /** If Content-Type header is not set, it is set to "application/octet-stream" */
  def respondBinary(bytes: Array[Byte]) {
    if (!response.containsHeader(CONTENT_TYPE))
      response.setHeader(CONTENT_TYPE, "application/octet-stream")

    val cb = ChannelBuffers.wrappedBuffer(bytes)
    if (response.isChunked) {
      writeHeaderOnFirstChunk
      val chunk = new DefaultHttpChunk(cb)
      channel.write(chunk)
    } else {
      HttpHeaders.setContentLength(response, bytes.length)
      response.setContent(cb)
      respond
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
  def respondFile(path: String) {
    XSendFile.setHeader(response, path)
    respond
  }

  /**
   * Sends a file from public directory in one of the entry (may be a JAR file)
   * in classpath.
   * If Content-Type header is not set, it is guessed from the file name.
   *
   * @param path Relative to an entry in classpath, without leading "/"
   */
  def respondResource(path: String) {
    XSendResource.setHeader(response, path)
    respond
  }

  //----------------------------------------------------------------------------

  def respondDefault404Page {
    XSendFile.set404Page(response)
    respond
  }

  def respondDefault500Page {
    XSendFile.set500Page(response)
    respond
  }

  //----------------------------------------------------------------------------

  def respondWebSocket(text: String) {
    channel.write(new TextWebSocketFrame(text))
  }
}
