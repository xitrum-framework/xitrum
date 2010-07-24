package xt.framework

import scala.xml.NodeSeq

abstract class View extends Helper {
  def render: NodeSeq

  /**
   * Renders another view.
   *
   * csasOrAs: "Articles#index" or "index"
   */
  def renderView(csasOrAs: String): NodeSeq = {
    val csas = normalizeCsasOrAs(csasOrAs)
    ViewCache.renderView(csas, this)
  }
}
