package xt.framework

import scala.collection.mutable.{Map, ListBuffer}
import scala.xml.Elem

trait View extends Helper {
  /**
   * Views will implement this method.
   */
  def render: Elem

  /**
   * Renders another view.
   */
  def renderView: Elem = {
    val cs   = params("controller")  // Articles
    val as   = params("action")      // index
    val csas = cs + "#" + as         // Articles#index
    renderView(csas)
  }

  /**
   * Renders another view.
   *
   * csasOrAs: "Articles#index" or "index"
   */
  def renderView(csasOrAs: String): Elem = {
    val csas = if (csasOrAs.indexOf("#") == -1)
      params("controller") + "#" + csasOrAs
    else
      csasOrAs

    val view = ViewCache.newView(csas, this)
    view.render
  }
}
