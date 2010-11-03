package xt.framework

import xt.middleware.Static

import java.io.File
import scala.xml.NodeSeq

import org.jboss.netty.buffer.ChannelBuffers
import org.jboss.netty.util.CharsetUtil

trait ControllerRender extends Helper {
  def layout: Option[String] = None

  //----------------------------------------------------------------------------

  def render: String = {
    val as = paramo("action").getOrElse(env("action404").asInstanceOf[String])
    render(as)
  }

  /**
   * @param csasOrAs: String in the pattern Articles#index or index
   * The layout is determined from the result of the layout method.
   */
  override def render(csasOrAs: String) = render(csasOrAs, layout)

  def render(csasOrAs: String, layout: Option[String]) = {
    val text = super.render(csasOrAs)
    renderText(text, layout)
  }

  //----------------------------------------------------------------------------

  def renderText(xml: NodeSeq): String = renderText(xml.toString)
  def renderText(xml: NodeSeq, layout: Option[String]): String = renderText(xml.toString, layout)

  def renderText(text: String): String = renderText(text, layout)

  def renderText(text: String, layout: Option[String]): String = {
    val t2 = layout match {
      case Some(csasOrAs2) =>
        at("content_for_layout", text)
        super.render(csasOrAs2)

      case None =>
        text
    }

    response.setContent(ChannelBuffers.copiedBuffer(t2, CharsetUtil.UTF_8))
    t2
  }

  def renderBinary(bytes: Array[Byte]) {
    response.setContent(ChannelBuffers.wrappedBuffer(bytes))
  }

  /**
   * @param path Starts with "/" for absolute path, otherwise it is relative to
   * the current working directory ((System.getProperty("user.dir"))
   */
  def renderFile(path: String) {
    val abs = if (path.startsWith("/"))
      path
    else
      System.getProperty("user.dir") + File.separator + path

    Static.renderFile(abs, channel, request, response, env)
  }
}
