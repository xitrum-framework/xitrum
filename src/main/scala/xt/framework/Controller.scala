package xt.framework

import org.jboss.netty.buffer.ChannelBuffer
import org.jboss.netty.util.CharsetUtil

trait Controller extends Helper {
  def layout: Option[String] = None

  def render: ChannelBuffer = {
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
  def render(csasOrAs: String, layout: Option[String]): ChannelBuffer = {
    val t1 = super.render(csasOrAs)
    val t2 = layout match {
      case Some(csasOrAs2) =>
        at("content_for_layout", t1.toString(CharsetUtil.UTF_8))
        super.render(csasOrAs2)

      case None =>
        t1
    }

    outputTextToResponse(t2)
  }

  //----------------------------------------------------------------------------

  def renderFile(path: String) {

  }

  //----------------------------------------------------------------------------

  private def outputTextToResponse(buffer: ChannelBuffer): ChannelBuffer = {
    response.setContent(buffer)
    buffer
  }
}
