package xitrum.view

import xitrum.Controller
import xitrum.controller.Action

/** Template engines should extend this trait and implement its methods. */
trait TemplateEngine {
  /** Renders the template associated with the action of the controller. */
  def renderTemplate(
    controller: Controller, action: Action,
    controllerName: String, actionName: String,
    options: Map[String, Any]
  ): String

  /** Renders the template associated with the controller. */
  def renderTemplate(
    controller: Controller, controllerClass: Class[_],
    options: Map[String, Any]
  ): String

  /** Renders the template fragment associated with the controller. */
  def renderFragment(
    controller: Controller, controllerClass: Class[_], fragment: String,
    options: Map[String, Any]
  ): String
}
