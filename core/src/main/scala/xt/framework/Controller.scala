package xt.framework

import org.jboss.netty.buffer.ChannelBuffers
import org.jboss.netty.util.CharsetUtil

trait Controller extends Helper {
  def layout: Option[String] = None

  def render: String = {
    val as = param("action").getOrElse(env("action404").asInstanceOf[String])
    render(as)
  }

  /**
   * @param csasOrAs: String in the pattern Articles#index or index
   * The layout is determined from the result of the layout method.
   */
  override def render(csasOrAs: String) = render(csasOrAs, layout)

  /**
   * @param layout None for no layout
   */
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

  def renderFile(path: String) {
  }
}
