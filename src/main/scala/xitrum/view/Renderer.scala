package xitrum.view

import java.io.File

import xitrum.{Config, Controller}
import xitrum.controller.Action
import xitrum.routing.Routes

trait Renderer {
  this: Controller =>

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
   * If you use Scalate and want to use template type other than the default type
   * configured in xitrum.conf, set options to Map("type" -> "jade", "mustache", "scaml", or "ssp")
   */
  def renderView(action: Action, customLayout: () => Any, options: Map[String, Any] = Map()): String = {
    val nonNullActionMethod = action.nonNullMethod
    val controllerClass     = nonNullActionMethod.getDeclaringClass
    val controllerName      = controllerClass.getName
    val actionName          = nonNullActionMethod.getName

    renderedView = Config.xitrum.templateEngine.renderTemplate(this, action, controllerName, actionName, options)
    customLayout.apply().toString
  }

  /**
   * Same as renderView(action, customLayout, options),
   * where action is currentAction.
   */
  def renderView(customLayout: () => Any, options: Map[String, Any]): String =
    renderView(currentAction, customLayout, options)

  def renderView(customLayout: () => Any): String =
    renderView(currentAction, customLayout, Map[String, Any]())

  /**
   * Same as renderView(action, customLayout, options),
   * where customLayout is the current controller's layout method.
   */
  def renderView(action: Action, options: Map[String, Any]): String =
    renderView(action, layout _, options)

  def renderView(action: Action): String =
    renderView(action, layout _, Map[String, Any]())

  /**
   * Same as renderView(action, customLayout, options),
   * where action is currentAction and customLayout is the current controller's layout method.
   */
  def renderView(options: Map[String, Any]): String =
    renderView(currentAction, layout _, options)

  def renderView(): String =
    renderView(currentAction, layout _, Map[String, Any]())

  //----------------------------------------------------------------------------

  def renderInlineView(inlineView: Any): String = {
    renderedView = inlineView
    val any = layout  // Call layout
    any.toString()
  }

  /**
   * Renders the template (typically the layout) associated with the controller class.
   *
   * If you use Scalate and want to use template type other than the default type
   * configured in xitrum.conf, set options to Map("type" -> "jade", "mustache", "scaml", or "ssp")
   *
   * @param controllerClass should be one of the parent classes of the current
   * controller because the current controller instance will be imported in the
   * template as "helper"
   */
  def renderViewNoLayout(controllerClass: Class[_], options: Map[String, Any]): String =
    Config.xitrum.templateEngine.renderTemplate(this, controllerClass, options)

  def renderViewNoLayout(controllerClass: Class[_]): String =
    Config.xitrum.templateEngine.renderTemplate(this, controllerClass, Map())

  //----------------------------------------------------------------------------

  /**
   * Renders the template fragment inside the directory associated with the
   * controller class.
   *
   * If you use Scalate and want to use template type other than the default type
   * configured in xitrum.conf, set options to Map("type" -> "jade", "mustache", "scaml", or "ssp")
   *
   * @param controllerClass should be one of the parent classes of the current
   * controller because the current controller instance will be imported in the
   * template as "helper"
   */
  def renderFragment(controllerClass: Class[_], fragment: String, options: Map[String, Any]): String =
    Config.xitrum.templateEngine.renderFragment(this, controllerClass, fragment, options)

  def renderFragment(controllerClass: Class[_], fragment: String): String =
    Config.xitrum.templateEngine.renderFragment(this, controllerClass, fragment, Map())

  /** Renders the fragment associated with the current controller class. */
  def renderFragment(fragment: String, options: Map[String, Any]): String =
    Config.xitrum.templateEngine.renderFragment(this, getClass, fragment, options)

  def renderFragment(fragment: String): String =
    Config.xitrum.templateEngine.renderFragment(this, getClass, fragment, Map())
}
