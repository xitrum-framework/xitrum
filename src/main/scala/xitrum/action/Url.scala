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
  def publicUrl(path: String): String = {
    val absPath     = Config.root + "/public/" + path
    val forceReload = Etag.forFile(absPath, None, true) match {
      case Etag.NotFound                           => Random.nextLong.toString
      case Etag.TooBig(file, mimeo)                => file.lastModified
      case Etag.Small(bytes, etag, mimeo, gzipped) => etag
    }

    val url = "/" + path
    Config.withBaseUrl(url + "?" + forceReload)
  }

  /**
   * Ex: publicUrl("jquery/2.1.1", "jquery.js", "jquery.min.js")
   *
   * @param devFile File to use in development environment
   * @param prodFile File to use in production environment
   */
  def publicUrl(directory: String, devFile: String, prodFile: String): String = {
    val file = if (Config.productionMode) prodFile else devFile
    val path = s"$directory/$file"
    publicUrl(path)
  }

  /** @param path Use "myapp/foo.js" to specify "META-INF/resources/webjars/myapp/foo.js" */
  def webJarsUrl(path: String): String = {
    val classPathPath = "META-INF/resources/webjars/" + path
    val forceReload = Etag.forResource(classPathPath, None, true) match {
      case Etag.NotFound                           => Random.nextLong.toString
      case Etag.Small(bytes, etag, mimeo, gzipped) => etag
    }
    Config.withBaseUrl("/webjars/" + path + "?" + forceReload)
  }

  /**
   * Ex: webJarsUrl("jquery/2.1.1", "jquery.js", "jquery.min.js")
   *
   * @param devFile File to use in development environment
   * @param prodFile File to use in production environment
   */
  def webJarsUrl(directory: String, devFile: String, prodFile: String): String = {
    val file = if (Config.productionMode) prodFile else devFile
    val path = s"$directory/$file"
    webJarsUrl(path)
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
