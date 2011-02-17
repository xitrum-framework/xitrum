package xitrum.action.view

import java.io.File
import org.jboss.netty.buffer.ChannelBuffers
import org.jboss.netty.handler.codec.http.HttpHeaders
import org.jboss.netty.util.CharsetUtil

import xitrum.action.Action

trait Renderer extends JQuery with JSCollector with Flash {
  this: Action =>

  def renderText(text: Any): String = {
    val s = text.toString

    // Content length is number of bytes, not Unicode characters!
    val cb = ChannelBuffers.copiedBuffer(s, CharsetUtil.UTF_8)
    HttpHeaders.setContentLength(response, cb.readableBytes)
    response.setContent(cb)
    respond

    s
  }

  //----------------------------------------------------------------------------

  def layout: Option[() => Any] = None

  def renderView(view: Any) {
    renderView(view, layout)
  }

  def renderView(view: Any, layout: Option[Any]) {
    layout match {
      case None           => renderText(view)
      case Some(function) =>
        at("contentForLayout") = view
        renderText(function.asInstanceOf[() => Any].apply)
    }
  }

  //----------------------------------------------------------------------------

  def renderBinary(bytes: Array[Byte]) {
    HttpHeaders.setContentLength(response, bytes.length)
    response.setContent(ChannelBuffers.wrappedBuffer(bytes))
    respond
  }

  /**
   * @param path Starts with "/" for absolute path, otherwise it is relative to
   * the current working directory (System.getProperty("user.dir"))
   */
  def renderFile(path: String) {
    val abs = if (path.startsWith("/"))
      path
    else
      System.getProperty("user.dir") + File.separator + path

    response.setHeader("X-Sendfile", abs)
    respond
  }
}
