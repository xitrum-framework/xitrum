package xitrum

import java.util.Properties
import java.nio.charset.Charset

import xitrum.action.env.session.SessionStore

object Config {
  def load(path: String): String = {
    val stream = getClass.getResourceAsStream(path)
    val len    = stream.available
    val bytes  = new Array[Byte](len)

    // Read whole file
    var total = 0
    while (total < len) {
      val bytesRead = stream.read(bytes, total, len - total)
      total += bytesRead
    }

    stream.close
    new String(bytes, "UTF-8")
  }

  def loadProperties(path: String): Properties = {
    val ret    = new Properties
    val stream = getClass.getClassLoader.getResourceAsStream(path)
    ret.load(stream)
    stream.close
    ret
  }

  //----------------------------------------------------------------------------

  val isProductionMode = (System.getProperty("xitrum.mode") == "production")

  // See xitrum.properties
  // Below are all "val"s

  val properties = loadProperties("xitrum.properties")

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

  var paramCharsetName = "UTF-8"
  var paramCharset     = Charset.forName(paramCharsetName)

  /**
   * Parameters are logged to access log
   * Comma separated list of sensitive parameters that should not be logged
   */
  var filteredParams = Array("password")

  /**
   * Xitrum checks the response Content-Type header to test if the response is
   * textual (text/html, text/plain etc.). If the response is big and gzip or
   * deflate Accept-Encoding header is set in the request, Xitrum will gzip or
   * deflate it. Xitrum compresses both static (see cacheSmallStaticFileMaxSizeInKB)
   * and dynamic response.
   */
  var compressBigTextualResponseMinSizeInKB = 50
}
