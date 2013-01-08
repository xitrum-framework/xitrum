package xitrum.view

import xitrum.Controller
import xitrum.controller.Action

trait TemplateEngine {
  def renderTemplate(
    controller: Controller, action: Action,
    controllerName: String, actionName: String,
    options: Map[String, Any]
  ): String

  def renderTemplate(
    controller: Controller, controllerClass: Class[_],
    options: Map[String, Any]
  ): String

  def renderFragment(
    controller: Controller, controllerClass: Class[_], fragment: String,
    options: Map[String, Any]
  ): String
}
