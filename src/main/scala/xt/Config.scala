package xt

import java.util.Properties
import java.nio.charset.Charset

import xt.vc.env.session.SessionStore

// Use lazy to avoid scala.UninitializedFieldError
object Config {
  lazy val isProductionMode = (System.getProperty("xitrum.mode") == "production")

  lazy val httpPort         = properties.getProperty("http_port",          "8080").toInt
  lazy val maxContentLength = properties.getProperty("max_content_length", "1048576").toInt  // default: 10MB

  lazy val paramCharsetName = properties.getProperty("param_charset", "UTF-8")
  lazy val paramCharset     = Charset.forName(paramCharsetName)

  lazy val filterParams = properties.getProperty("filter_params", "password").split(", ")

  lazy val filesMaxSize     = properties.getProperty("files_max_size",     "102400").toInt

  lazy val sessionIdName         = properties.getProperty("session_id_name",           "JSESSIONID")
  lazy val sessionIdInCookieOnly = properties.getProperty("session_id_in_cookie_only", "true") == "true"
  lazy val sessionStore: SessionStore = {
    val className = properties.getProperty("session_store", "xt.vc.env.session.CookieSessionStore")
    Class.forName(className).newInstance.asInstanceOf[SessionStore]
  }

  private lazy val properties = {
    val ret = new Properties
    val stream = getClass.getClassLoader.getResourceAsStream("xitrum.properties")
    if (stream != null) ret.load(stream)
    ret
  }
}
