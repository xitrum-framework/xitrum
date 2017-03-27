package xitrum.action

import scala.util.Random

import xitrum.{Config, Action, SockJsAction, WebSocketAction}
import xitrum.etag.Etag

object Url {
  /**
    * Ex: publicUrl("jquery/3.2.1/dist/jquery.js")
    *
    * @param path Relative to the "public" directory, without leading "/"
    */
  def publicUrl(path: String): String = {
    val absPath     = xitrum.root + "/public/" + path
    val forceReload = Etag.forFile(absPath, None, gzipped = true) match {
      case Etag.NotFound                           => Random.nextLong.toString
      case Etag.TooBig(file, mimeo)                => file.lastModified
      case Etag.Small(bytes, etag, mimeo, gzipped) => etag
    }

    val url = "/" + path
    Config.withBaseUrl(url + "?" + forceReload)
  }

  /**
    * Ex: publicUrl("jquery/3.2.1/dist/jquery.js", "jquery/3.2.1/dist/jquery.min.js")
    *
    * @param devPath File path to use in development mode, relative to the "public" directory, without leading "/"
    * @param prodPath File path to use in production mode, relative to the "public" directory, without leading "/"
    */
  def publicUrl(devPath: String, prodPath: String): String = {
    val path = if (Config.productionMode) prodPath else devPath
    publicUrl(path)
  }

  /**
    * Ex: publicUrl("jquery/3.2.1/dist", "jquery.js", "jquery.min.js")
    *
    * @param directory Relative to the "public" directory, without leading "/"
    * @param devFile File in the directory to use in development mode
    * @param prodFile File in the directory to use in production mode
    */
  def publicUrl(directory: String, devFile: String, prodFile: String): String = {
    val file = if (Config.productionMode) prodFile else devFile
    val path = s"$directory/$file"
    publicUrl(path)
  }

  /** @param path Use "myapp/foo.js" to specify "META-INF/resources/webjars/myapp/foo.js" */
  def webJarsUrl(path: String): String = {
    val classPathPath = "META-INF/resources/webjars/" + path
    val forceReload = Etag.forResource(classPathPath, None, gzipped = true) match {
      case Etag.NotFound                           => Random.nextLong.toString
      case Etag.Small(bytes, etag, mimeo, gzipped) => etag
    }
    Config.withBaseUrl("/webjars/" + path + "?" + forceReload)
  }

  /**
    * Ex: webJarsUrl("jquery/3.2.1/dist/jquery.js", "jquery/3.2.1/dist/jquery.min.js")
    *
    * @param devPath File path to use in development mode
    * @param prodPath File path to use in production mode
    */
  def webJarsUrl(devPath: String, prodPath: String): String = {
    val path = if (Config.productionMode) prodPath else devPath
    webJarsUrl(path)
  }

  /**
    * Ex: webJarsUrl("jquery/3.2.1/dist", "jquery.js", "jquery.min.js")
    *
    * @param devFile File in the directory to use in development mode
    * @param prodFile File in the directory to use in production mode
    */
  def webJarsUrl(directory: String, devFile: String, prodFile: String): String = {
    val file = if (Config.productionMode) prodFile else devFile
    val path = s"$directory/$file"
    webJarsUrl(path)
  }

  //----------------------------------------------------------------------------

  def url(className: String, params: (String, Any)*): String = Config.routes.reverseMappings(className).url(params.toMap)

  def url[T <: Action : Manifest](params: (String, Any)*): String = {
    val className = manifest[T].runtimeClass.getName
    url(className, params:_*)
  }
  def url[T <: Action : Manifest]: String = url[T]()

  def sockJsUrl[T <: SockJsAction : Manifest]: String = {
    val klass = manifest[T].runtimeClass.asInstanceOf[Class[SockJsAction]]
    Config.routes.sockJsRouteMap.findPathPrefix(klass)
  }
}

trait Url {
  this: Action =>

  lazy val absUrlPrefixWithoutScheme: String = {
    val portSuffix =
      if ((isSsl && serverPort == 443) || (!isSsl && serverPort == 80))
        ""
      else
        ":" + serverPort
    serverName + portSuffix + Config.baseUrl
  }

  lazy val absUrlPrefix: String          = scheme          + "://" + absUrlPrefixWithoutScheme
  lazy val absWebSocketUrlPrefix: String = webSocketScheme + "://" + absUrlPrefixWithoutScheme

  // iPhone Safari throws error "location mismatch" if the request URL is
  // http://example.com/ws
  // but the response URL here is
  // ws://example.com:80/ws
  lazy val absWebSocketRequestUrl: String = absWebSocketUrlPrefix + request.uri

  //----------------------------------------------------------------------------

  /**
    * Ex: publicUrl("jquery/3.2.1/dist/jquery.js")
    *
    * @param path Relative to the "public" directory, without leading "/"
    */
  def publicUrl(path: String) = Url.publicUrl(path)

  /**
    * Ex: publicUrl("jquery/3.2.1/dist/jquery.js", "jquery/3.2.1/dist/jquery.min.js")
    *
    * @param devPath File path to use in development mode, relative to the "public" directory, without leading "/"
    * @param prodPath File path to use in production mode, relative to the "public" directory, without leading "/"
    */
  def publicUrl(devPath: String, prodPath: String) = Url.publicUrl(devPath, prodPath)

  /**
    * Ex: publicUrl("jquery/3.2.1/dist", "jquery.js", "jquery.min.js")
    *
    * @param directory Relative to the "public" directory, without leading "/"
    * @param devFile File in the directory to use in development mode
    * @param prodFile File in the directory to use in production mode
    */
  def publicUrl(directory: String, devFile: String, prodFile: String) = Url.publicUrl(directory, devFile, prodFile)

  /** @param path Use "myapp/foo.js" to specify "META-INF/resources/webjars/myapp/foo.js" */
  def webJarsUrl(path: String) = Url.webJarsUrl(path)

  /**
    * Ex: webJarsUrl("jquery/3.2.1/dist/jquery.js", "jquery/3.2.1/dist/jquery.min.js")
    *
    * @param devPath File path to use in development mode
    * @param prodPath File path to use in production mode
    */
  def webJarsUrl(devPath: String, prodPath: String) = Url.webJarsUrl(devPath, prodPath)

  /**
    * Ex: webJarsUrl("jquery/3.2.1/dist", "jquery.js", "jquery.min.js")
    *
    * @param devFile File in the directory to use in development mode
    * @param prodFile File in the directory to use in production mode
    */
  def webJarsUrl(directory: String, devFile: String, prodFile: String) = Url.webJarsUrl(directory, devFile, prodFile)

  def url(params: (String, Any)*): String = Url.url(getClass.getName, params:_*)
  lazy val url: String = Url.url(getClass.getName)

  def url[T <: Action : Manifest](params: (String, Any)*) = Url.url[T](params:_*)
  def url[T <: Action : Manifest]: String = url[T]()

  def sockJsUrl[T <: SockJsAction : Manifest] = Url.sockJsUrl[T]

  //----------------------------------------------------------------------------

  def absUrl(params: (String, Any)*): String = absUrlPrefix + Url.url(getClass.getName, params:_*)
  lazy val absUrl: String = absUrlPrefix + Url.url(getClass.getName)

  def absUrl[T <: Action : Manifest](params: (String, Any)*): String = absUrlPrefix + url[T](params:_*)
  def absUrl[T <: Action : Manifest]: String = absUrl[T]()

  def absWebSocketUrl(params: (String, Any)*): String = absWebSocketUrlPrefix + Url.url(getClass.getName, params:_*)
  lazy val absWebSocketUrl: String = absWebSocketUrlPrefix + Url.url(getClass.getName)

  def absWebSocketUrl[T <: WebSocketAction : Manifest](params: (String, Any)*): String = {
    val klass = manifest[T].runtimeClass.asInstanceOf[Class[Action]]
    absWebSocketUrlPrefix + Config.routes.reverseMappings(klass.getName).url(params.toMap)
  }
  def absWebSocketUrl[T <: WebSocketAction : Manifest]: String = absWebSocketUrl[T]()

  def absSockJsUrl[T <: SockJsAction : Manifest]: String = absUrlPrefix + sockJsUrl[T]
}
