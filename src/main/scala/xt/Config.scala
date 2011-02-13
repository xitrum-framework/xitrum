package xt

import java.util.Properties
import java.nio.charset.Charset

import xt.vc.env.session.SessionStore

// Use lazy to avoid scala.UninitializedFieldError
object Config {
  val isProductionMode = (System.getProperty("xitrum.mode") == "production")

  val properties = {
    val ret    = new Properties
    val stream = getClass.getClassLoader.getResourceAsStream("xitrum.properties")
    if (stream != null) ret.load(stream)
    ret
  }

  val httpPort         = properties.getProperty("http_port",          "8080").toInt
  val maxContentLength = properties.getProperty("max_content_length", "1048576").toInt  // default: 10MB

  val paramCharsetName = properties.getProperty("param_charset",      "UTF-8")
  val paramCharset     = Charset.forName(paramCharsetName)

  val filterParams     = properties.getProperty("filter_params",      "password").split(", ")

  val filesMaxSize     = properties.getProperty("files_max_size",     "102400").toInt

  val sessionMarker    = properties.getProperty("session_marker",     "_session")
  val sessionStore: SessionStore = {
    val className = properties.getProperty("session_store", "xt.vc.env.session.CookieSessionStore")
    Class.forName(className).newInstance.asInstanceOf[SessionStore]
  }

  val secureBase64Key  = properties.getProperty("secure_base64_key",  "1234567890123456")
}
