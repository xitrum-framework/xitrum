package xt.vc
package controller

import java.io.File
import scala.xml.NodeSeq

import org.jboss.netty.buffer.ChannelBuffers
import org.jboss.netty.util.CharsetUtil
import org.jboss.netty.handler.codec.http.HttpHeaders

trait Renderer extends Helper {
  this: Controller =>

  def layout: Option[String] = None

  //----------------------------------------------------------------------------

  def renderView: String = renderView(layout)

  def renderView(layout: Option[String]): String = {
    val as = param("action")
    renderView(as, layout)
  }

  /**
   * @param csasOrAs: String in the pattern Articles#index or index
   * The layout is determined from the result of the layout method.
   */
  def renderView(csasOrAs: String): String = renderView(csasOrAs, layout)

  def renderView(csasOrAs: String, layout: Option[String]) = {
    val text = super.render(csasOrAs)
    renderText(text, layout)
  }

  //----------------------------------------------------------------------------

  def renderText(text: Any): String = renderText(text, layout)

  def renderText(text: Any, layout: Option[String]): String = {
    val t2 = layout match {
      case Some(csasOrAs2) =>
        at("content_for_layout") = text.toString
        render(csasOrAs2)

      case None =>
        text.toString
    }

    // Content length is number of bytes, not Unicode characters!
    val cb = ChannelBuffers.copiedBuffer(t2, CharsetUtil.UTF_8)
    HttpHeaders.setContentLength(response, cb.readableBytes)
    response.setContent(cb)
    respond

    t2
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
