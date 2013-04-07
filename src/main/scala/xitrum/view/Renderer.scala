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
  def renderView(action: Class[_ <: Action], customLayout: () => Any, options: Map[String, Any] = Map()): String = {
    renderedView = Config.xitrum.templateEngine.renderTemplate(action, options)
    customLayout.apply().toString
  }

  /**
   * Same as renderView(action, customLayout, options),
   * where action is currentAction.
   */
  def renderView(customLayout: () => Any, options: Map[String, Any]): String =
    renderView(currentAction.getClass, customLayout, options)

  def renderView(customLayout: () => Any): String =
    renderView(currentAction.getClass, customLayout, Map[String, Any]())

  /**
   * Same as renderView(action, customLayout, options),
   * where customLayout is the current controller's layout method.
   */
  def renderView(action: Class[_ <: Action], options: Map[String, Any]): String =
    renderView(action, layout _, options)

  def renderView(action: Class[_ <: Action]): String =
    renderView(action, layout _, Map[String, Any]())

  /**
   * Same as renderView(action, customLayout, options),
   * where action is currentAction and customLayout is the current controller's layout method.
   */
  def renderView(options: Map[String, Any]): String =
    renderView(currentAction.getClass, layout _, options)

  def renderView(): String =
    renderView(currentAction.getClass, layout _, Map[String, Any]())

  //----------------------------------------------------------------------------

  def renderInlineView(inlineView: Any): String = {
    renderedView = inlineView
    val any = layout  // Call layout
    any.toString()
  }

  /**
   * Renders the template (typically the layout) associated with the controller class.
   *
   * @param controllerClass should be one of the parent classes of the current
   * controller because the current controller instance will be imported in the
   * template as "helper"
   *
   * @param options specific to the configured template engine
   */
  def renderViewNoLayout(actionClass: Class[_], options: Map[String, Any]): String =
    "Config.xitrum.templateEngine.renderTemplate(this, actionClass, options)"

  def renderViewNoLayout(actionClass: Class[_]): String =
    "Config.xitrum.templateEngine.renderTemplate(this, actionClass, Map())"
}
