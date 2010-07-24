package xt.framework

import scala.collection.mutable.{Map, ListBuffer}
import scala.collection.JavaConversions
import scala.xml.NodeSeq

import org.jboss.netty.buffer.ChannelBuffers
import org.jboss.netty.util.CharsetUtil

trait Controller extends Helper {
  def layout: Option[String] = None

  def render: String = {
    val as = param("action").get
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
  def render(csasOrAs: String, layout: Option[String]): String = {
    val t1: String = super.render(csasOrAs)
    val t2: String = layout match {
      case Some(csasOrAs) =>
        at("content_for_layout", t1)
        super.render(csasOrAs)

      case None =>
        t1
    }

    outputTextToResponse(t2)
  }

  //----------------------------------------------------------------------------

  def renderFile(path: String) {

  }

  //----------------------------------------------------------------------------

  private def outputTextToResponse(text: String): String = {
    response.setContent(ChannelBuffers.copiedBuffer(text, CharsetUtil.UTF_8))
    text
  }
}
