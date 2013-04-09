package xitrum.action

import scala.util.Random

import xitrum.{Config, Action}
import xitrum.etag.Etag
import xitrum.routing.Routes

trait UrlFor {
  this: Action =>

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

  //----------------------------------------------------------------------------

  def url(params: (String, Any)*) = Routes.routes.reverseMappings(getClass).url(params:_*)
  lazy val url: String = url()

  def absoluteUrl(params: (String, Any)*) = absoluteUrlPrefix + url(params:_*)
  def absoluteUrl: String = absoluteUrl()

  def webSocketAbsoluteUrl(params: (String, Any)*) = webSocketAbsoluteUrlPrefix + url(params:_*)
  def webSocketAbsoluteUrl: String = webSocketAbsoluteUrl()

  //----------------------------------------------------------------------------

  def url[T: Manifest](params: (String, Any)*) = {
    val actionClass = manifest[T].runtimeClass.asInstanceOf[Class[Action]]
    Routes.routes.reverseMappings(actionClass).url(params:_*)
  }
  def url[T: Manifest]: String = url[T]()

  def absoluteUrl[T: Manifest](params: (String, Any)*) = absoluteUrlPrefix + url[T](params:_*)
  def absoluteUrl[T: Manifest]: String = absoluteUrl[T]()

  def webSocketAbsoluteUrl[T: Manifest](params: (String, Any)*) = {
    val actionClass = manifest[T].runtimeClass.asInstanceOf[Class[Action]]
    webSocketAbsoluteUrlPrefix + url[T](params:_*)
  }
  def webSocketAbsoluteUrl[T: Manifest]: String = webSocketAbsoluteUrl[T]()
}
