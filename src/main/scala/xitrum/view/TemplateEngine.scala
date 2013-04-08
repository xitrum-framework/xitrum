package xitrum.view

import xitrum.ActionEnv

/**
 * Template engines should extend this trait and implement its methods.
 * On startup, an instance of the configured template engine is created and used
 * for every rendering request. Thus it should be thread-safe.
 */
trait TemplateEngine {
  /**
   * Renders the template associated with the actionClass (not action).
   *
   * Ex: When actionClass = myapp.SiteIndex and Scalate template
   * engine is used, by default the template path will be:
   * src/main/scalate/myapp/SiteIndex.jade
   *
   * @param options specific to the configured template engine
   */
  def renderTemplate(actionClass: Class[_ <: ActionEnv], action: ActionEnv, options: Map[String, Any]): String
}
