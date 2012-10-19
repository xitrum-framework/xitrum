package xitrum.scope.session

import scala.collection.mutable.{Map => MMap}

import org.jboss.netty.handler.codec.http.DefaultCookie

import xitrum.{Config, Logger}
import xitrum.scope.request.ExtEnv
import xitrum.util.SecureBase64

/** Compress big session cookie to try to avoid the 4KB limit. */
class CookieSessionStore extends SessionStore with Logger {
  def store(session: Session, extEnv: ExtEnv) {
    // See "restore" method above
    // Convert to immutable because mutable cannot always be deserialize later!
    val immutableMap = session.toMap

    // Most browsers do not support cookie > 4KB
    val serialized = SecureBase64.encrypt(immutableMap, true)
    val cookieSize = serialized.length
    if (cookieSize > 4 * 1024) {
      logger.error("Cookie size = " + cookieSize + " > 4KB limit: " + immutableMap)
      return
    }

    val cookiePath = Config.withBaseUrl("/")
    extEnv.cookies.get(Config.config.session.cookieName) match {
      case Some(cookie) =>
        cookie.setHttpOnly(true)
        cookie.setPath(cookiePath)
        cookie.setValue(serialized)

      case None =>
        // DefaultCookie has max age of Integer.MIN_VALUE by default,
        // which means the cookie will be removed when user terminates browser
        val cookie = new DefaultCookie(Config.config.session.cookieName, serialized)
        cookie.setHttpOnly(true)
        cookie.setPath(cookiePath)
        extEnv.cookies.add(cookie)
    }
  }

  def restore(extEnv: ExtEnv): Session = {
    // Cannot always get cookie, decrypt, deserialize, and type casting due to program changes etc.
    extEnv.cookies.get(Config.config.session.cookieName) match {
      case None =>
        MMap[String, Any]()
      case Some(cookie) =>
        val base64String = cookie.getValue
        SecureBase64.decrypt(base64String, true) match {
          case None =>
            MMap[String, Any]()
          case Some(value) =>
            val immutableMap = try {
              // See "store" method below
              value.asInstanceOf[Map[String, Any]]
            } catch {
              case _ =>
                MMap[String, Any]()
            }
            val ret = MMap[String, Any]()
            ret ++= immutableMap
            ret
       }
    }
  }
}
