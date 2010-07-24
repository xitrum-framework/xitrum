package xt.framework

import scala.collection.mutable.{Map, ListBuffer}
import scala.collection.JavaConversions
import scala.xml.NodeSeq

import org.jboss.netty.buffer.ChannelBuffers
import org.jboss.netty.util.CharsetUtil

trait Controller extends Helper {
  def layout: Option[String] = None

  def renderView {
    val cs   = param("controller").get  // Articles
    val as   = param("action").get      // index
    val csas = cs + "#" + as            // Articles#index
    renderView(csas)
  }

  /**
   * The layout is determined from the result of the layout method.
   */
  def renderView(csasOrAs: String) {
    renderView(csasOrAs, layout)
  }

  /**
   * @param layout None for no layout
   */
  def renderView(csasOrAs: String, layout: Option[String]) {
    val xml1 = callView(csasOrAs)
    val xml2 = layout match {
      case Some(csasOrAs) =>
        at("content_for_layout", xml1)
        callView(csasOrAs)

      case None =>
        xml1
    }
    outputTextToResponse(xml2.toString)
  }

  //----------------------------------------------------------------------------

  def renderFile(path: String) {

  }

  //----------------------------------------------------------------------------

  /**
   * @param csasOrAs Points to a view or a layout (a layout is just a normal view)
   */
  private def callView(csasOrAs: String): NodeSeq = {
    val csas = normalizeCsasOrAs(csasOrAs)
    ViewCache.renderView(csas, this)
  }

  private def outputTextToResponse(text: String) {
    response.setContent(ChannelBuffers.copiedBuffer(text, CharsetUtil.UTF_8))
  }
}
