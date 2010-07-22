package xt.framework

import scala.collection.mutable.{Map, ListBuffer}
import scala.collection.JavaConversions
import scala.xml.Elem

trait Controller extends Helper {
  def layout: Option[String] = None

  def render: Elem = {
    val cs   = params("controller")  // Articles
    val as   = params("action")      // index
    val csas = cs + "#" + as         // Articles#index
    render(csas)
  }

  /**
   * csasOrAs: "Articles#index" or "index"
   */
  def render(csasOrAs: String): Elem = {
    val csas = if (csasOrAs.indexOf("#") == -1)
      params("controller") + "#" + csasOrAs
    else
      csasOrAs

    val view = ViewCache.newView(csas)
    view.render
  }
}
