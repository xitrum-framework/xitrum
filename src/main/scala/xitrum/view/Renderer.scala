package xitrum.view

import java.io.File

import xitrum.{Config, Controller}
import xitrum.controller.Action
import xitrum.routing.Routes

trait Renderer {
  this: Controller =>

  var renderedTemplate: Any = null

  def layout = renderedTemplate

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
   * Renders the template associated with the action.
   *
   * If you use Scalate and want to use template type other than the default type
   * configured in xitrum.conf, set options to Map("type" -> "jade", "mustache", "scaml", or "ssp")
   */
  def renderTemplate(action: Action, options: Map[String, Any] = Map()): String = {
    val nonNullActionMethod = if (action.method == null) Routes.lookupMethod(action.route) else action.method
    val controllerClass     = nonNullActionMethod.getDeclaringClass
    val controllerName      = controllerClass.getName
    val actionName          = nonNullActionMethod.getName
    Config.config.templateEngine.renderTemplate(this, action, controllerName, actionName, options)
  }

  /**
   * Same as renderTemplate(action, options),
   * where action is currentAction.
   */
  def renderTemplate(options: Map[String, Any]): String =
    renderTemplate(currentAction, options)

  def renderTemplate(): String =
    renderTemplate(currentAction, Map[String, Any]())

  //----------------------------------------------------------------------------

  /**
   * Renders the template associated with an action to "renderedTemplate",
   * then calls the layout function.
   *
   * If you use Scalate and want to use template type other than the default type
   * configured in xitrum.conf, set options to Map("type" -> "jade", "mustache", "scaml", or "ssp")
   */
  def renderTemplateWithLayout(action: Action, customLayout: () => Any, options: Map[String, Any] = Map()): String = {
    renderedTemplate = renderTemplate(action, options)
    customLayout.apply().toString
  }

  /**
   * Same as renderTemplateWithLayout(action, customLayout, options),
   * where action is currentAction.
   */
  def renderTemplateWithLayout(customLayout: () => Any, options: Map[String, Any]): String =
    renderTemplateWithLayout(currentAction, customLayout, options)

  def renderTemplateWithLayout(customLayout: () => Any): String =
    renderTemplateWithLayout(currentAction, customLayout, Map[String, Any]())

  /**
   * Same as renderTemplateWithLayout(action, customLayout, options),
   * where customLayout is the current controller's layout method.
   */
  def renderTemplateWithLayout(action: Action, options: Map[String, Any]): String =
    renderTemplateWithLayout(action, layout _, options)

  def renderTemplateWithLayout(action: Action): String =
    renderTemplateWithLayout(action, layout _, Map[String, Any]())

  /**
   * Same as renderTemplateWithLayout(action, customLayout, options),
   * where action is currentAction and customLayout is the current controller's layout method.
   */
  def renderTemplateWithLayout(options: Map[String, Any]): String =
    renderTemplateWithLayout(currentAction, layout _, options)

  def renderTemplateWithLayout(): String =
    renderTemplateWithLayout(currentAction, layout _, Map[String, Any]())

  //----------------------------------------------------------------------------

  def renderWithLayout(inlineTemplate: Any): String = {
    renderedTemplate = inlineTemplate
    layout.toString()
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
  def renderTemplate(controllerClass: Class[_], options: Map[String, Any]): String =
    Config.config.templateEngine.renderTemplate(this, controllerClass, options)

  def renderTemplate(controllerClass: Class[_]): String =
    Config.config.templateEngine.renderTemplate(this, controllerClass, Map())

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
    Config.config.templateEngine.renderFragment(this, controllerClass, fragment, options)

  def renderFragment(controllerClass: Class[_], fragment: String): String =
    Config.config.templateEngine.renderFragment(this, controllerClass, fragment, Map())

  /** Renders the fragment associated with the current controller class. */
  def renderFragment(fragment: String, options: Map[String, Any]): String =
    Config.config.templateEngine.renderFragment(this, getClass, fragment, options)

  def renderFragment(fragment: String): String =
    Config.config.templateEngine.renderFragment(this, getClass, fragment, Map())
}
