package xitrum.view

import java.io.File

import xitrum.{Config, Action}
import xitrum.routing.Routes

trait Renderer {
  this: Action =>

  var renderedView: Any = null

  def layout = renderedView

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

  //----------------------------------------------------------------------------

  /**
   * Renders the template associated with an action to "renderedTemplate",
   * then calls the layout function.
   *
   * @param options specific to the configured template engine
   */
  def renderView(customLayout: () => Any, location: Class[_ <: Action], options: Map[String, Any]): String = {
    renderedView = Config.xitrum.templateEngine.renderView(location, this, options)
    customLayout.apply().toString
  }

  def renderView(location: Class[_ <: Action], options: Map[String, Any]): String =
    renderView(layout _, location, options)

  def renderView(customLayout: () => Any, options: Map[String, Any]): String =
    renderView(customLayout, getClass, options)

  def renderView(customLayout: () => Any, location: Class[_ <: Action]): String =
    renderView(customLayout, location, Map())

  def renderView(customLayout: () => Any): String =
    renderView(customLayout, getClass, Map())

  def renderView(location: Class[_ <: Action]): String =
    renderView(layout _, location, Map())

  def renderView(options: Map[String, Any]): String =
    renderView(layout _, getClass, options)

  def renderView(): String =
    renderView(layout _, getClass, Map())

  //----------------------------------------------------------------------------

  def renderViewNoLayout(location: Class[_ <: Action], options: Map[String, Any]): String =
    Config.xitrum.templateEngine.renderView(location, this, options)

  def renderViewNoLayout(location: Class[_ <: Action]): String =
    Config.xitrum.templateEngine.renderView(location, this, Map())

  def renderViewNoLayout(options: Map[String, Any]): String =
    Config.xitrum.templateEngine.renderView(getClass, this, options)

  def renderViewNoLayout(): String =
    Config.xitrum.templateEngine.renderView(getClass, this, Map())

  //----------------------------------------------------------------------------

  def renderFragment(location: Class[_ <: Action], fragment: String, options: Map[String, Any]): String =
    Config.xitrum.templateEngine.renderFragment(location, fragment, this, options)

  def renderFragment(fragment: String, options: Map[String, Any]): String =
    Config.xitrum.templateEngine.renderFragment(getClass, fragment, this, options)

  def renderFragment(location: Class[_ <: Action], fragment: String): String =
    Config.xitrum.templateEngine.renderFragment(location, fragment, this, Map())

  def renderFragment(fragment: String): String =
    Config.xitrum.templateEngine.renderFragment(getClass, fragment, this, Map())

  //----------------------------------------------------------------------------

  def renderInlineView(inlineView: Any): String = {
    renderedView = inlineView
    val any = layout  // Call layout
    any.toString()
  }
}
