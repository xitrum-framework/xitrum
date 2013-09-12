package xitrum.action

import scala.util.Random

import xitrum.{Config, Action, SockJsActor, WebSocketActor}
import xitrum.etag.Etag
import xitrum.handler.up.PublicFileServer

trait UrlFor {
  this: Action =>

  /** @param path Relative to the "public" directory, without leading "/" */
  def publicUrl(path: String) = {
    val absPath     = Config.root + "/public/" + path
    val forceReload = Etag.forFile(absPath, true) match {
      case Etag.NotFound                           => Random.nextLong.toString
      case Etag.TooBig(file)                       => file.lastModified
      case Etag.Small(bytes, etag, mimeo, gzipped) => etag
    }

    // staticFileUrlPrefix: Starts and stops with "/", like "/static/", if any
    val url = Config.xitrum.request.staticFileUrlPrefix match {
      case None         => "/"    + path
      case Some(prefix) => prefix + path
    }
    Config.withBaseUrl(url + "?" + forceReload)
  }

  /** @param path Relative to an entry in classpath, without leading "/" */
  def resourceUrl(path: String) = {
    val classPathPath = "public/" + path
    val forceReload = Etag.forResource(classPathPath, true) match {
      case Etag.NotFound                           => Random.nextLong.toString
      case Etag.Small(bytes, etag, mimeo, gzipped) => etag
    }
    Config.withBaseUrl("/resources/public/" + path + "?" + forceReload)
  }

  //----------------------------------------------------------------------------

  def url(params: (String, Any)*) = Config.routes.reverseMappings(getClass).url(params:_*)
  lazy val url: String = url()

  def absUrl(params: (String, Any)*) = absUrlPrefix + url(params:_*)
  lazy val absUrl: String = absUrl()

  def webSocketAbsUrl(params: (String, Any)*) = webSocketAbsUrlPrefix + url(params:_*)
  lazy val webSocketAbsUrl: String = webSocketAbsUrl()

  //----------------------------------------------------------------------------

  def url[T <: Action : Manifest](params: (String, Any)*) = {
    val klass = manifest[T].runtimeClass.asInstanceOf[Class[Action]]
    Config.routes.reverseMappings(klass).url(params:_*)
  }
  def url[T <: Action : Manifest]: String = url[T]()

  def absUrl[T <: Action : Manifest](params: (String, Any)*) = absUrlPrefix + url[T](params:_*)
  def absUrl[T <: Action : Manifest]: String = absUrl[T]()

  //----------------------------------------------------------------------------

  def webSocketAbsUrl[T <: WebSocketActor : Manifest](params: (String, Any)*) = {
    val klass = manifest[T].runtimeClass.asInstanceOf[Class[Action]]
    webSocketAbsUrlPrefix + Config.routes.reverseMappings(klass).url(params:_*)
  }
  def webSocketAbsUrl[T <: WebSocketActor : Manifest]: String = webSocketAbsUrl[T]()

  //----------------------------------------------------------------------------

  def sockJsUrl[T <: SockJsActor : Manifest] = {
    val klass = manifest[T].runtimeClass.asInstanceOf[Class[SockJsActor]]
    Config.routes.sockJsRouteMap.findPathPrefix(klass)
  }

  def sockJsAbsUrl[T <: SockJsActor : Manifest] = absUrlPrefix + sockJsUrl[T]
}
