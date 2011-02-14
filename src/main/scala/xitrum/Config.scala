package xitrum

import java.util.Properties
import java.nio.charset.Charset

import xitrum.vc.env.session.SessionStore

// Use lazy to avoid scala.UninitializedFieldError
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

  val properties       = loadProperties("xitrum.properties")

  val httpPort         = properties.getProperty("http_port",          "8080").toInt
  val maxContentLength = properties.getProperty("max_content_length", "1048576").toInt  // default: 10MB

  val paramCharsetName = properties.getProperty("param_charset",      "UTF-8")
  val paramCharset     = Charset.forName(paramCharsetName)

  val filterParams     = properties.getProperty("filter_params",      "password").split(", ")

  val filesMaxSize     = properties.getProperty("files_max_size",     "102400").toInt

  val sessionMarker    = properties.getProperty("session_marker",     "_session")
  val sessionStore     = {
    val className = properties.getProperty("session_store", "xitrum.vc.env.session.CookieSessionStore")
    Class.forName(className).newInstance.asInstanceOf[SessionStore]
  }

  val secureBase64Key  = properties.getProperty("secure_base64_key",  "1234567890123456")
}
