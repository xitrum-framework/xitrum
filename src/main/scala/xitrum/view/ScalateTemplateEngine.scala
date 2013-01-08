package xitrum.view

import java.io.File

import xitrum.{Config, Controller}
import xitrum.controller.Action

class ScalateTemplateEngine extends TemplateEngine {
  def renderTemplate(
    controller: Controller, action: Action,
    controllerName: String, actionName: String,
    options: Map[String, Any]
  ) = Scalate.renderTemplate(controller, action, controllerName, actionName, options)

  def renderTemplate(
    controller: Controller, controllerClass: Class[_],
    options: Map[String, Any]
  ) = Scalate.renderTemplate(controller, controllerClass, options)

  def renderFragment(
    controller: Controller, controllerClass: Class[_], fragment: String,
    options: Map[String, Any]
  ) = Scalate.renderFragment(controller, controllerClass, fragment, options)
}
