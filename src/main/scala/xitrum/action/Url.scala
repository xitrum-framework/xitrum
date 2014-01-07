package xitrum.action

import scala.util.Random

import xitrum.{Config, Action, SockJsAction, WebSocketAction}
import xitrum.etag.Etag
import xitrum.handler.inbound.PublicFileServer

trait Url {
  this: Action =>

  lazy val absUrlPrefixWithoutScheme = {
    val portSuffix =
      if ((isSsl && serverPort == 443) || (!isSsl && serverPort == 80))
        ""
      else
        ":" + serverPort
    serverName + portSuffix + Config.baseUrl
  }

  lazy val absUrlPrefix          = scheme          + "://" + absUrlPrefixWithoutScheme
  lazy val webSocketAbsUrlPrefix = webSocketScheme + "://" + absUrlPrefixWithoutScheme

  // iPhone Safari throws error "location mismatch" if the request URL is
  // http://example.com/ws
  // but the response URL here is
  // ws://example.com:80/ws
  lazy val webSocketAbsRequestUrl = webSocketAbsUrlPrefix + request.getUri

  //----------------------------------------------------------------------------

  /** @param path Relative to the "public" directory, without leading "/" */
  def publicUrl(path: String) = {
    val absPath     = Config.root + "/public/" + path
    val forceReload = Etag.forFile(absPath, true) match {
      case Etag.NotFound                           => Random.nextLong.toString
      case Etag.TooBig(file)                       => file.lastModified
      case Etag.Small(bytes, etag, mimeo, gzipped) => etag
    }

    val url = "/" + path
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

  def url(params: (String, Any)*) = Config.routesReverseMappings(getClass).url(params.toMap)
  lazy val url: String = url()

  def absUrl(params: (String, Any)*) = absUrlPrefix + url(params:_*)
  lazy val absUrl: String = absUrl()

  def webSocketAbsUrl(params: (String, Any)*) = webSocketAbsUrlPrefix + url(params:_*)
  lazy val webSocketAbsUrl: String = webSocketAbsUrl()

  //----------------------------------------------------------------------------

  def url[T <: Action : Manifest](params: (String, Any)*) = {
    val klass = manifest[T].runtimeClass.asInstanceOf[Class[Action]]
    val path  = Config.routesReverseMappings(klass).url(params.toMap)

    // See xitrum.js
    if (klass == classOf[xitrum.js])
      path + "?" + Etag.forString(xitrum.js.body)
    else
      path
  }
  def url[T <: Action : Manifest]: String = url[T]()

  def absUrl[T <: Action : Manifest](params: (String, Any)*) = absUrlPrefix + url[T](params:_*)
  def absUrl[T <: Action : Manifest]: String = absUrl[T]()

  //----------------------------------------------------------------------------

  def webSocketAbsUrl[T <: WebSocketAction : Manifest](params: (String, Any)*) = {
    val klass = manifest[T].runtimeClass.asInstanceOf[Class[Action]]
    webSocketAbsUrlPrefix + Config.routesReverseMappings(klass).url(params.toMap)
  }
  def webSocketAbsUrl[T <: WebSocketAction : Manifest]: String = webSocketAbsUrl[T]()

  //----------------------------------------------------------------------------

  def sockJsUrl[T <: SockJsAction : Manifest] = {
    val klass = manifest[T].runtimeClass.asInstanceOf[Class[SockJsAction]]
    Config.routes.sockJsRouteMap.findPathPrefix(klass)
  }

  def sockJsAbsUrl[T <: SockJsAction : Manifest] = absUrlPrefix + sockJsUrl[T]
}
