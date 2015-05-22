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

  /**
   * Ex: publicUrl("jquery/2.1.4/jquery.js")
   *
   * @param path Relative to the "public" directory, without leading "/"
   */
  def publicUrl(path: String): String = {
    val absPath     = xitrum.root + "/public/" + path
    val forceReload = Etag.forFile(absPath, None, true) match {
      case Etag.NotFound                           => Random.nextLong.toString
      case Etag.TooBig(file, mimeo)                => file.lastModified
      case Etag.Small(bytes, etag, mimeo, gzipped) => etag
    }

    val url = "/" + path
    Config.withBaseUrl(url + "?" + forceReload)
  }

  /**
   * Ex: publicUrl("jquery.js", "jquery.min.js")
   *
   * @param devPath File path to use in development mode, relative to the "public" directory, without leading "/"
   * @param prodPath File path to use in production mode, relative to the "public" directory, without leading "/"
   */
  def publicUrl(devPath: String, prodPath: String): String = {
    val path = if (Config.productionMode) prodPath else devPath
    publicUrl(path)
  }

  /**
   * Ex: publicUrl("jquery/2.1.4", "jquery.js", "jquery.min.js")
   *
   * @param directory Relative to the "public" directory, without leading "/"
   * @param devPath File path to use in development mode, relative to "directory" above
   * @param prodPath File path to use in production mode, relative to "directory" above
   */
  def publicUrl(directory: String, devPath: String, prodPath: String): String = {
    val path1 = if (Config.productionMode) prodPath else devPath
    val path2 = s"$directory/$path1"
    publicUrl(path2)
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
   * Ex: webJarsUrl("jquery/2.1.4", "jquery.js", "jquery.min.js")
   *
   * @param devFile File to use in development mode
   * @param prodFile File to use in production mode
   */
  def webJarsUrl(directory: String, devFile: String, prodFile: String): String = {
    val file = if (Config.productionMode) prodFile else devFile
    val path = s"$directory/$file"
    webJarsUrl(path)
  }

  //----------------------------------------------------------------------------

  def url(params: (String, Any)*) = Config.routes.reverseMappings(getClass.getName).url(params.toMap)
  lazy val url: String = url()

  def absUrl(params: (String, Any)*) = absUrlPrefix + url(params:_*)
  lazy val absUrl: String = absUrl()

  def webSocketAbsUrl(params: (String, Any)*) = webSocketAbsUrlPrefix + url(params:_*)
  lazy val webSocketAbsUrl: String = webSocketAbsUrl()

  //----------------------------------------------------------------------------

  def url[T <: Action : Manifest](params: (String, Any)*) = {
    val className = manifest[T].runtimeClass.getName
    val path      = Config.routes.reverseMappings(className).url(params.toMap)

    // See xitrum.js
    if (className == classOf[xitrum.js].getName)
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
    webSocketAbsUrlPrefix + Config.routes.reverseMappings(klass.getName).url(params.toMap)
  }
  def webSocketAbsUrl[T <: WebSocketAction : Manifest]: String = webSocketAbsUrl[T]()

  //----------------------------------------------------------------------------

  def sockJsUrl[T <: SockJsAction : Manifest] = {
    val klass = manifest[T].runtimeClass.asInstanceOf[Class[SockJsAction]]
    Config.routes.sockJsRouteMap.findPathPrefix(klass)
  }

  def sockJsAbsUrl[T <: SockJsAction : Manifest] = absUrlPrefix + sockJsUrl[T]
}
