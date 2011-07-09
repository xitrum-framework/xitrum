package xitrum

import java.io.{InputStream, FileInputStream}
import java.util.Properties
import java.nio.charset.Charset

import xitrum.scope.session.SessionStore

object Config extends Logger {
  def bytesFromInputStream(is: InputStream): Array[Byte] = {
    val len   = is.available
    val bytes = new Array[Byte](len)
    var total = 0
    while (total < len) {
      val bytesRead = is.read(bytes, total, len - total)
      total += bytesRead
    }
    is.close
    bytes
  }

  /**
   * @param path Relative to one of the elements in CLASSPATH, without leading "/"
   */
  def loadStringFromClasspath(path: String): String = {
    val stream = getClass.getClassLoader.getResourceAsStream(path)
    val bytes  = bytesFromInputStream(stream)
    new String(bytes, "UTF-8")
  }

  /**
   * @param path Relative to one of the elements in CLASSPATH, without leading "/"
   */
  def loadPropertiesFromClasspath(path: String): Properties = {
    // http://www.javaworld.com/javaworld/javaqa/2003-08/01-qa-0808-property.html?page=2
    val stream = getClass.getClassLoader.getResourceAsStream(path)

    val ret = new Properties
    ret.load(stream)
    stream.close
    ret
  }

  def loadPropertiesFromFile(path: String): Properties = {
    val stream = new FileInputStream(path)
    val ret = new Properties
    ret.load(stream)
    stream.close
    ret
  }

  //----------------------------------------------------------------------------

  val isProductionMode = (System.getProperty("xitrum.mode") == "production")

  // See xitrum.properties
  // Below are all "val"s

  val properties = {
    try {
      loadPropertiesFromClasspath("xitrum.properties")
    } catch {
      case _ =>
        try {
          loadPropertiesFromFile("config/xitrum.properties")
        } catch {
          case _ =>
            logger.error("Could not load xitrum.properties from CLASSPATH or from config/xitrum.properties")
            System.exit(-1)
            null
        }
    }
  }

  val httpPort = properties.getProperty("http_port").toInt

  val proxyIpso: Option[Array[String]] = {
    val s = properties.getProperty("proxy_ips")
    if (s == null) None else Some(s.split(",").map(_.trim))
  }

  val baseUri = properties.getProperty("base_uri", "")

  val compressResponse = {
    val s = properties.getProperty("compress_response")
    if (s == null || s == "false") false else true
  }

  val sessionStore  = {
    val className = properties.getProperty("session_store")
    Class.forName(className).newInstance.asInstanceOf[SessionStore]
  }

  val cookieName = properties.getProperty("cookie_name")

  val secureKey = properties.getProperty("secure_key")

  //----------------------------------------------------------------------------

  // Below are all "var"s so that application developers may change the defaults

  var maxRequestContentLengthInMB = 10

  /**
   * For speed, to avoid checking file existance on every request, public files
   * should have URL pattern /public/...
   *
   * favicon.ico: http://en.wikipedia.org/wiki/Favicon
   * robots.txt:  http://en.wikipedia.org/wiki/Robots_exclusion_standard
   */
  var publicFilesNotBehindPublicUrl = List("favicon.ico", "robots.txt")

  /**
   * Xitrum can serve static files (request URL in the form /public/...
   * or /responses/public/... or there is X-Sendfile in the response header),
   * and it caches small static files in memory.
   */
  var cacheSmallStaticFileMaxSizeInKB = 512

  /**
   * Xitrum checks the response Content-Type header to test if the response is
   * textual (text/html, text/plain etc.). If the response is big and gzip or
   * deflate Accept-Encoding header is set in the request, Xitrum will gzip or
   * deflate it. Xitrum compresses both static (see cacheSmallStaticFileMaxSizeInKB)
   * and dynamic response.
   */
  var compressBigTextualResponseMinSizeInKB = 10

  var paramCharsetName = "UTF-8"
  var paramCharset     = Charset.forName(paramCharsetName)

  /**
   * Parameters are logged to access log
   * Comma separated list of sensitive parameters that should not be logged
   */
  var filteredParams = Array("password")

  /**
   * When there is trouble (high load on startup ect.), the response may not be
   * OK. If the response is specified to be cached, we should only cache it
   * for a short time.
   */
  var non200ResponseCacheTTLInSecs = 30
}
