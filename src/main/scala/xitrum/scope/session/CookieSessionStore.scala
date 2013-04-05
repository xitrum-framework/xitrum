package xitrum.scope.session

import scala.collection.mutable.{Map => MMap}

import org.jboss.netty.handler.codec.http.DefaultCookie

import xitrum.{Config, Logger}
import xitrum.util.SecureUrlSafeBase64

/** Compress big session cookie to try to avoid the 4KB limit. */
class CookieSessionStore extends SessionStore with Logger {
  def restore(env: SessionEnv): Session = {
    // Cannot always get cookie, decrypt, deserialize, and type casting due to program changes etc.
    env.requestCookies.get(Config.xitrum.session.cookieName) match {
      case None =>
        MMap[String, Any]()

      // See "store" method to know why this map needs to be immutable
      case Some(encryptedImmutableMap) =>
        SecureUrlSafeBase64.decrypt(encryptedImmutableMap, true) match {
          case None =>
            MMap[String, Any]()

          case Some(any) =>
            val immutableMap = try {
              any.asInstanceOf[Map[String, Any]]
            } catch {
              case scala.util.control.NonFatal(e) =>
                MMap[String, Any]()
            }

            // Convert to mutable map
            val ret = MMap[String, Any]()
            ret ++= immutableMap
            ret
       }
    }
  }

  def store(session: Session, env: SessionEnv) {
    val sessionCookieName = Config.xitrum.session.cookieName
    if (session.isEmpty) {
      // If session cookie has been sent by browser, send back session cookie
      // with max age = 0 so that browser will delete it immediately
      if (env.requestCookies.isDefinedAt(sessionCookieName)) {
        val cookie = new DefaultCookie(sessionCookieName, "0")
        cookie.setHttpOnly(true)
        cookie.setMaxAge(0)
        env.responseCookies.append(cookie)
      }
    } else {
      // See "restore" method
      // Convert to immutable because mutable cannot always be deserialized later!
      val immutableMap = session.toMap

      // Most browsers do not support cookie > 4KB
      val serialized = SecureUrlSafeBase64.encrypt(immutableMap, true)
      val cookieSize = serialized.length
      if (cookieSize > 4 * 1024) {
        logger.error("Cookie size = " + cookieSize + " > 4KB limit: " + immutableMap)
        return
      }

      val previousSessionCookieValueo = env.requestCookies.get(sessionCookieName)
      if (previousSessionCookieValueo.isEmpty || previousSessionCookieValueo.get != serialized) {
        // DefaultCookie has max age of Integer.MIN_VALUE by default,
        // which means the cookie will be removed when user terminates browser
        val cookie = new DefaultCookie(sessionCookieName, serialized)
        cookie.setHttpOnly(true)
        env.responseCookies.append(cookie)
      }
    }
  }
}
