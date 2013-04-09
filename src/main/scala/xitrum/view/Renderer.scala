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
  def renderView(customLayout: () => Any, actionClass: Class[_ <: Action], options: Map[String, Any]): String = {
    renderedView = Config.xitrum.templateEngine.renderTemplate(actionClass, this, options)
    customLayout.apply().toString
  }

  def renderView(customLayout: () => Any, options: Map[String, Any]): String =
    renderView(customLayout, getClass, options)

  def renderView(customLayout: () => Any): String =
    renderView(customLayout, getClass, Map())

  def renderView(actionClass: Class[_ <: Action], options: Map[String, Any]): String =
    renderView(layout _, actionClass, options)

  def renderView(actionClass: Class[_ <: Action]): String =
    renderView(layout _, actionClass, Map())

  def renderView(options: Map[String, Any]): String =
    renderView(layout _, getClass, options)

  def renderView(): String =
    renderView(layout _, getClass, Map())

  //----------------------------------------------------------------------------

  def renderInlineView(inlineView: Any): String = {
    renderedView = inlineView
    val any = layout  // Call layout
    any.toString()
  }

  def renderViewNoLayout(actionClass: Class[_ <: Action], options: Map[String, Any]): String =
    Config.xitrum.templateEngine.renderTemplate(actionClass, this, options)

  def renderViewNoLayout(actionClass: Class[_ <: Action]): String =
    Config.xitrum.templateEngine.renderTemplate(actionClass, this, Map())

  def renderViewNoLayout(): String =
    Config.xitrum.templateEngine.renderTemplate(getClass, this, Map())
}
