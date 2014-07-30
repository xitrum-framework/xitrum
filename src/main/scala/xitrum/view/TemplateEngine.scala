package xitrum.view

import scala.util.control.NonFatal
import com.typesafe.config.{Config => TConfig}
import xitrum.{Action, Config, DualConfig, Log}

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

object TemplateEngine {
  /**
   * Template config in xitrum.conf can be in 2 forms:
   *
   * template = my.template.Engine
   *
   * Or if the template engine needs additional options:
   *
   * template {
   *   "my.template.Engine" {
   *     option1 = value1
   *     option2 = value2
   *   }
   * }
   */
  def loadFromConfig(): Option[TemplateEngine] = {
    if (Config.xitrum.config.hasPath("template")) {
      try {
        val engine = DualConfig.getClassInstance[TemplateEngine](Config.xitrum.config, "template")
        engine.start()
        Some(engine)
      } catch {
        case NonFatal(e) =>
          Config.exitOnStartupError("Could not load template engine, please check config/xitrum.conf", e)
          None
      }
    } else {
      Log.info("No template engine is configured")
      None
    }
  }
}
