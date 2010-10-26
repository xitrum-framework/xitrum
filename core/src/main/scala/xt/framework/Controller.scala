package xt.framework

import java.io.File

import org.jboss.netty.buffer.ChannelBuffers
import org.jboss.netty.util.CharsetUtil

import xt.middleware.Static
import xt.server.Handler

trait Controller extends Helper {
  def layout: Option[String] = None

  def beforeFilter = true

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

  //----------------------------------------------------------------------------

  def ignoreResponse {
    Handler.ignoreResponse(env)
  }

  def respond {
    Handler.respond(channel, request, response)
  }
}
