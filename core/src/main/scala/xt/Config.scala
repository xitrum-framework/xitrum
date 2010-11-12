package xt

import xt.vc.env.session.SessionStore

import java.util.Properties

object Config {
  lazy val isProductionMode = (System.getProperty("xitrum.mode") == "production")

  lazy val httpPort         = properties.getProperty("http_port",          "8080").toInt
  lazy val maxContentLength = properties.getProperty("max_content_length", "1048576").toInt  // default: 10MB

  lazy val filterParams = properties.getProperty("filter_params", "password").split(", ")

  lazy val filesEhcacheName = properties.getProperty("files_ehcache_name", "XitrumFiles")
  lazy val filesMaxSize     = properties.getProperty("files_max_size",     "102400").toInt

  lazy val sessionIdName         = properties.getProperty("session_id_name",           "JSESSIONID")
  lazy val sessionIdInCookieOnly = properties.getProperty("session_id_in_cookie_only", "true") == "true"
  lazy val sessionStore: SessionStore = {
    val className = properties.getProperty("session_store", "xt.vc.helper.session.EhcacheSessionStore")
    Class.forName(className).newInstance.asInstanceOf[SessionStore]
  }

  /**
   * Allow this to be accessed from other places, so that other configs can be
   * stored in xitrum.properties
   */
  lazy val properties = {
    val ret = new Properties
    val stream = getClass.getClassLoader.getResourceAsStream("xitrum.properties")
    if (stream != null) ret.load(stream)
    ret
  }
}
