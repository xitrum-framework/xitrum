package xt.framework

import scala.collection.mutable.{Map, ListBuffer}
import scala.collection.JavaConversions
import scala.xml.{Elem, Unparsed}

import org.jboss.netty.buffer.ChannelBuffers
import org.jboss.netty.util.CharsetUtil

trait Controller extends Helper {
  def layout: Option[String] = None

  def renderView {
    val cs   = param("controller").get  // Articles
    val as   = param("action").get      // index
    val csas = cs + "#" + as             // Articles#index
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
    val text  = renderViewToString(csasOrAs)
    renderText(text, layout)
  }

  /**
   * The layout is determined from the result of the layout method.
   */
  def renderText(xml: Elem) {
    renderText(xml, layout)
  }

  def renderText(xml: Elem, layout: Option[String]) {
    renderText(xml.toString, layout)
  }

  /**
   * The layout is determined from the result of the layout method.
   */
  def renderText(text: String) {
    renderText(text, layout)
  }

  def renderText(text: String, layout: Option[String]) {
    layout match {
      case Some(csasOrAs) =>
        at("content_for_layout", Unparsed(text))
        val text2 = renderViewToString(csasOrAs)
        outputTextToResponse(text2)

      case None =>
        outputTextToResponse(text)
    }
  }

  /**
   * @param csasOrAs Points to a view or a layout (a layout is just a normal view)
   */
  private def renderViewToString(csasOrAs: String) = {
    val csas = if (csasOrAs.indexOf("#") == -1)
      param("controller").get + "#" + csasOrAs
    else
      csasOrAs

    val view = ViewCache.newView(csas, this)
    view.render.toString
  }

  private def outputTextToResponse(text: String) {
    response.setContent(ChannelBuffers.copiedBuffer(text, CharsetUtil.UTF_8))
  }

  def renderFile(path: String) {

  }
}
