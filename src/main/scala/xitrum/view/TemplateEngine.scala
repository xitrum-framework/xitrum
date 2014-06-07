package xitrum.view

import xitrum.Action

/**
 * Template engines should extend this trait and implement its methods.
 * On startup, an instance of the configured template engine is created and used
 * for every rendering request. Thus it should be thread-safe.
 */
trait TemplateEngine {
  /**
   * Called once when Xitrum server starts. If necessary the template engine
   * should allocate its resources here.
   */
  def start()

  /**
   * Called once when Xitrum server stops. If necessary the template engine
   * should release its resources here.
   */
  def stop()

  /**
   * Called multiple times, but only in development mode, when Xitrum detects
   * that there's a change in directory target/scala-<version>/classes. Because,
   * for example, maybe there's new class file or existing class file has been
   * changed, if necessary the template engine should reload when it renders on
   * the next request.
   *
   * Because this method is only called in development mode, to optimize, the
   * template engine may act differently for development mode and production
   * mode. Use xitrum.Config.productionMode to check which mode the program is
   * running in.
   */
  def reloadOnNextRender()

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
