package xt

import java.util.Properties
import java.nio.charset.Charset

import net.sf.ehcache.CacheManager

import xt.vc.env.session.SessionStore

object Config {
  val isProductionMode = (System.getProperty("xitrum.mode") == "production")

  val httpPort         = properties.getProperty("http_port",          "8080").toInt
  val maxContentLength = properties.getProperty("max_content_length", "1048576").toInt  // default: 10MB

  val paramCharsetName = properties.getProperty("param_charset", "UTF-8")
  val paramCharset     = Charset.forName(paramCharsetName)

  val filterParams = properties.getProperty("filter_params", "password").split(", ")

  val filesEhcacheName = properties.getProperty("files_ehcache_name", "XitrumFiles")
  val filesMaxSize     = properties.getProperty("files_max_size",     "102400").toInt

  val sessionIdName         = properties.getProperty("session_id_name",           "JSESSIONID")
  val sessionIdInCookieOnly = properties.getProperty("session_id_in_cookie_only", "true") == "true"
  val sessionStore: SessionStore = {
    val className = properties.getProperty("session_store", "xt.vc.env.session.EhcacheSessionStore")
    Class.forName(className).newInstance.asInstanceOf[SessionStore]
  }

  val sessionEhcache = {
    val name = Config.properties.getProperty("sessions_ehcache_name", "XitrumSessions")
    CacheManager.getInstance.getCache(name)
  }

  private val properties = {
    val ret = new Properties
    val stream = getClass.getClassLoader.getResourceAsStream("xitrum.properties")
    if (stream != null) ret.load(stream)
    ret
  }
}
