package xitrum.controller

import scala.util.Random

import xitrum.{Config, Controller}
import xitrum.etag.Etag

trait UrlFor {
  this: Controller =>

  // Use "def" instead of "lazy val" to prevent this route from being picked by RouteCollector
  def currentRoute = handlerEnv.route

  /** @param path Relative to the "public" directory, without leading "/" */
  def urlForPublic(path: String) = {
    val absPath     = Config.root + "/public/" + path
    val forceReload = Etag.forFile(absPath, true) match {
      case Etag.NotFound                           => Random.nextLong.toString
      case Etag.TooBig(file)                       => file.lastModified
      case Etag.Small(bytes, etag, mimeo, gzipped) => etag
    }
    Config.withBaseUrl("/" + path + "?" + forceReload)
  }

  /** @param path Relative to an entry in classpath, without leading "/" */
  def urlForResource(path: String) = {
    val classPathPath = "public/" + path
    val forceReload = Etag.forResource(classPathPath, true) match {
      case Etag.NotFound                           => Random.nextLong.toString
      case Etag.Small(bytes, etag, mimeo, gzipped) => etag
    }
    Config.withBaseUrl("/resources/public/" + path + "?" + forceReload)
  }
}
