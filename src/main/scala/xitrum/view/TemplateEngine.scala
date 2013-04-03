package xitrum.view

import xitrum.Controller
import xitrum.controller.Action

/**
 * Template engines should extend this trait and implement its methods.
 * On startup, an instance of the configured template engine is created and used
 * for every rendering request. Thus it should be thread-safe.
 */
trait TemplateEngine {
  /**
   * Renders the template associated with the action of the controller.
   *
   * Ex: When controller = myapp.Site, action = index,
   * by default the Scalate template path will be:
   * src/main/scalate/myapp/Site/index.jade
   */
  def renderTemplate(
    controller: Controller, action: Action,
    controllerName: String, actionName: String,
    options: Map[String, Any]
  ): String

  /**
   * Renders the template associated with the controller.
   *
   * Ex: When controller = myapp.Site,
   * by default the Scalate template path will be:
   * src/main/scalate/myapp/Site.jade
   */
  def renderTemplate(
    controller: Controller, controllerClass: Class[_],
    options: Map[String, Any]
  ): String

  /**
   * Renders the template fragment associated with the controller.
   *
   * Ex: When controller = myapp.Site, fragment = "footer",
   * by default the Scalate template path will be:
   * src/main/scalate/myapp/Site/_footer.jade
   */
  def renderFragment(
    controller: Controller, controllerClass: Class[_], fragment: String,
    options: Map[String, Any]
  ): String
}
