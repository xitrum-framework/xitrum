package xitrum.action.view

import java.io.File
import scala.xml.{Elem, NodeBuffer}

import org.jboss.netty.buffer.ChannelBuffers
import org.jboss.netty.handler.codec.http.HttpHeaders
import HttpHeaders.Names.CONTENT_TYPE

import xitrum.Config
import xitrum.action.Action
import xitrum.handler.updown.XSendfile

trait Renderer extends JQuery with JSCollector with Flash with I18n {
  this: Action =>

  def renderText(text: Any, contentType: String = null): String = {
    // Set content type automatically
    if (contentType != null) {
      response.setHeader(CONTENT_TYPE, contentType)
    } else if (!response.containsHeader(CONTENT_TYPE)) {
      if (text.isInstanceOf[Elem] || text.isInstanceOf[NodeBuffer]) {
        response.setHeader(CONTENT_TYPE, "application/xml")
      } else {
        response.setHeader(CONTENT_TYPE, "text/plain")
      }
    }

    val ret = text.toString

    // Content length is number of bytes, not characters!
    val cb = ChannelBuffers.copiedBuffer(ret, Config.paramCharset)
    HttpHeaders.setContentLength(response, cb.readableBytes)
    response.setContent(cb)
    respond

    ret
  }

  //----------------------------------------------------------------------------

  def layout: Option[() => Any] = None

  def renderView(view: Any) {
    renderView(view, layout)
  }

  def renderView(view: Any, layout: Option[Any]) {
    layout match {
      case None =>
        renderText(view, "text/html")

      case Some(function) =>
        at("contentForLayout") = view
        renderText(function.asInstanceOf[() => Any].apply, "text/html")
    }
  }

  //----------------------------------------------------------------------------

  def renderBinary(bytes: Array[Byte]) {
    HttpHeaders.setContentLength(response, bytes.length)
    response.setContent(ChannelBuffers.wrappedBuffer(bytes))
    respond
  }

  /**
   * Sends a file using X-Sendfile.
   *
   * @param path Starts with "/" for absolute path, otherwise it is relative to
   * the current working directory (System.getProperty("user.dir")).
   * path is not sanitized. To sanitize, use xitrum.Sanitizer.
   */
  def renderFile(path: String) {
    val abs = if (path.startsWith("/")) path else System.getProperty("user.dir") + File.separator + path
    XSendfile.setHeader(response, abs)
    respond
  }

  def render404Page {
    XSendfile.set404Page(response)
    respond
  }
}
