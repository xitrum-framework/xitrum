package xitrum.view

import xitrum.Action

/**
 * Template engines should extend this trait and implement its methods.
 * On startup, an instance of the configured template engine is created and used
 * for every rendering request. Thus it should be thread-safe.
 */
trait TemplateEngine {
  def start()
  def stop()

  /**
   * Renders the template at the location identified by the given action class.
   *
   * Ex: When location = myapp.SiteIndex and Scalate template
   * engine is used, by default the template path will be:
   * src/main/scalate/myapp/SiteIndex.jade
   *
   * @param location the action class used to identify the template location
   *
   * @param options specific to the configured template engine
   */
  def renderView(location: Class[_ <: Action], currentAction: Action, options: Map[String, Any]): String

  /**
   * Renders the template at the location identified by the package of the given
   * action class and the given fragment.
   *
   * Ex: When location = myapp.ArticleNew, fragment = form and Scalate template
   * engine is used, by default the template path will be:
   * src/main/scalate/myapp/_form.jade
   *
   * @param location the action class used to identify the template location
   *
   * @param options specific to the configured template engine
   */
  def renderFragment(location: Class[_ <: Action], fragment: String, currentAction: Action, options: Map[String, Any]): String
}
