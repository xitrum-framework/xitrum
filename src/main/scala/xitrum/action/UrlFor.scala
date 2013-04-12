package xitrum.action

import scala.util.Random

import xitrum.{Config, Action, SockJsActor}
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

  def absUrl(params: (String, Any)*) = absUrlPrefix + url(params:_*)
  lazy val absUrl: String = absUrl()

  def webSocketAbsUrl(params: (String, Any)*) = webSocketAbsUrlPrefix + url(params:_*)
  lazy val webSocketAbsUrl: String = webSocketAbsUrl()

  //----------------------------------------------------------------------------

  def url[T <: Action : Manifest](params: (String, Any)*) = {
    val actionClass = manifest[T].runtimeClass.asInstanceOf[Class[Action]]
    Routes.routes.reverseMappings(actionClass).url(params:_*)
  }
  def url[T <: Action : Manifest]: String = url[T]()

  def absUrl[T <: Action : Manifest](params: (String, Any)*) = absUrlPrefix + url[T](params:_*)
  def absUrl[T <: Action : Manifest]: String = absUrl[T]()

  def webSocketAbsUrl[T <: Action : Manifest](params: (String, Any)*) = {
    val actionClass = manifest[T].runtimeClass.asInstanceOf[Class[Action]]
    webSocketAbsUrlPrefix + url[T](params:_*)
  }
  def webSocketAbsUrl[T <: Action : Manifest]: String = webSocketAbsUrl[T]()

  //----------------------------------------------------------------------------

  def sockJsUrl[T <: SockJsActor : Manifest] = {
    val handlerClass = manifest[T].runtimeClass.asInstanceOf[Class[SockJsActor]]
    Routes.sockJsPathPrefix(handlerClass)
  }

  def sockJsAbsUrl[T <: SockJsActor : Manifest] = absUrlPrefix + sockJsUrl[T]
}
