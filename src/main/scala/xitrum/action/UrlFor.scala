package xitrum.action

import scala.util.Random

import xitrum.{Config, Action, SockJsHandler}
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
  def absUrl: String = absUrl()

  def webSocketAbsUrl(params: (String, Any)*) = webSocketAbsUrlPrefix + url(params:_*)
  def webSocketAbsUrl: String = webSocketAbsUrl()

  //----------------------------------------------------------------------------

  def url[T: Manifest](params: (String, Any)*) = {
    val actionClass = manifest[T].runtimeClass.asInstanceOf[Class[Action]]
    Routes.routes.reverseMappings(actionClass).url(params:_*)
  }
  def url[T: Manifest]: String = url[T]()

  def absUrl[T: Manifest](params: (String, Any)*) = absUrlPrefix + url[T](params:_*)
  def absUrl[T: Manifest]: String = absUrl[T]()

  def webSocketAbsUrl[T: Manifest](params: (String, Any)*) = {
    val actionClass = manifest[T].runtimeClass.asInstanceOf[Class[Action]]
    webSocketAbsUrlPrefix + url[T](params:_*)
  }
  def webSocketAbsUrl[T: Manifest]: String = webSocketAbsUrl[T]()

  //----------------------------------------------------------------------------

  def sockJsUrl[T: Manifest] = {
    val handlerClass = manifest[T].runtimeClass.asInstanceOf[Class[SockJsHandler]]
    Routes.sockJsPathPrefix(handlerClass)
  }

  def sockJsAbsUrl[T: Manifest] = absUrlPrefix + sockJsUrl[T]
}
