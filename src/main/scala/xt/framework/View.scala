package xt.framework

class View extends Helper {
  /**
   * Renders another view.
   */
  def renderView: String = {
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
  def renderView(csasOrAs: String): String = {
    renderView(this, csasOrAs)
  }

  def renderView(view: View, csasOrAs: String): String = {
    val caa = csasOrAs.split("#")
    val csas = if (caa.size == 2)
      caa(0).toLowerCase + "/" + caa(1)
    else
      param("controller").get.toLowerCase + "/" + csasOrAs

    val path = "view" + "/" + csas + ".scaml"
    Scalate.render(view, path)
  }
}
