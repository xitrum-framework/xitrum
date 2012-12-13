package xitrum.view

import java.io.File

import xitrum.{Controller, Config}

trait Renderer {
  this: Controller =>

  var renderedView: Any = null

  def layout = renderedView

  //----------------------------------------------------------------------------

  /**
   * Renders Scalate template file relative to src/main/view/scalate directory.
   * The current controller instance will be imported in the template as "helper".
   */
  def renderScalate(relPath: String) = Scalate.renderFile(this, relPath)

  /**
   * Renders Scalate template file with the path:
   * src/main/view/scalate/<the/given/controller/Class>.<templateType>
   *
   * @param controllerClass should be one of the parent classes of the current controller
   *                        because the current controller instance will be imported
   *                        in the template as "helper"
   * @param templateType "jade", "mustache", "scaml", or "ssp"
   */
  def renderScalate(controllerClass: Class[_], templateType: String): String = {
    val relPath = controllerClass.getName.replace('.', File.separatorChar) + "." + templateType
    renderScalate(relPath)
  }

  /**
   * Same as renderScalate(controllerClass, templateType),
   * where templateType is as configured in xitrum.json.
   */
  def renderScalate(controllerClass: Class[_]): String =
    renderScalate(controllerClass, Config.config.scalate)

  //----------------------------------------------------------------------------

  /**
   * Renders Scalate template file with the path:
   * src/main/view/scalate/<the/given/controller/Class>/_<fragment>.<templateType>
   *
   * @param controllerClass should be one of the parent classes of the current controller
   *                        because the current controller instance will be imported
   *                        in the template as "helper"
   * @param templateType "jade", "mustache", "scaml", or "ssp"
   */
  def renderFragment(controllerClass: Class[_], fragment: String, templateType: String): String = {
    val relPath = controllerClass.getName.replace('.', File.separatorChar) + File.separatorChar + "_" + fragment + "." + templateType
    renderScalate(relPath)
  }

  /**
   * Same as renderFragment(controllerClass, fragment, templateType),
   * where templateType is as configured in xitrum.json.
   */
  def renderFragment(controllerClass: Class[_], fragment: String): String =
    renderFragment(controllerClass, fragment, Config.config.scalate)

  /** Renders a fragment of the current controller. */
  def renderFragment(fragment: String): String =
    renderFragment(getClass, fragment)

  //----------------------------------------------------------------------------

  def renderEventSource(data: Any, event: String = "message"): String = {
    val builder = new StringBuilder

    if (event != "message") {
      builder.append("event: ")
      builder.append(event)
      builder.append("\n")
    }

    val lines = data.toString.split("\n")
    val n = lines.length
    for (i <- 0 until n) {
      if (i < n - 1) {
        builder.append("data: ")
        builder.append(lines(i))
        builder.append("\n")
      } else {
        builder.append("data: ")
        builder.append(lines(i))
      }
    }

    builder.append("\r\n\r\n")
    builder.toString
  }
}
