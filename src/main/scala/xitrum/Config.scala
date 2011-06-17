package xitrum

import java.io.{InputStream, FileInputStream}
import java.util.Properties
import java.nio.charset.Charset

import xitrum.scope.session.SessionStore

object Config extends Logger {
  def bytesFromStreamAndClose(stream: InputStream): Array[Byte] = {
    val len   = stream.available
    val bytes = new Array[Byte](len)
    var total = 0
    while (total < len) {
      val bytesRead = stream.read(bytes, total, len - total)
      total += bytesRead
    }
    stream.close
    bytes
  }

  /**
   * @param path Relative to one of the elements in CLASSPATH, without leading "/"
   */
  def loadStringFromClasspath(path: String): String = {
    val stream = getClass.getClassLoader.getResourceAsStream(path)
    val bytes  = bytesFromStreamAndClose(stream)
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

  /** None if Xitrum need not to process HTTPS */
  val httpsPort: Option[Int] = {
    val s = properties.getProperty("https_port")
    if (s == null) None else Some(s.toInt)
  }

  val compressResponse = {
    val s = properties.getProperty("compress_response")
    if (s == null || s == "false") false else true
  }

  val sessionMarker = properties.getProperty("session_marker")
  val sessionStore  = {
    val className = properties.getProperty("session_store")
    Class.forName(className).newInstance.asInstanceOf[SessionStore]
  }

  val secureKey = properties.getProperty("secure_key")

  //----------------------------------------------------------------------------

  // Below are all "var"s so that application developers may change the defaults

  var maxRequestContentLengthInMB = 10

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
