package xitrum.scope.session

import scala.collection.mutable.{Map => MMap}

import io.netty.handler.codec.http.cookie.DefaultCookie

import xitrum.Log
import xitrum.Config
import xitrum.util.SeriDeseri

/** Compress big session cookie to try to avoid the 4KB limit. */
class CookieSessionStore extends SessionStore {
  def start() {}

  def stop() {}

  def store(session: Session, env: SessionEnv) {
    if (session.isEmpty) {
      // If session cookie has been sent by browser, send back session cookie
      // with max age = 0 so that browser will delete it immediately
      if (env.requestCookies.isDefinedAt(Config.xitrum.session.cookieName)) {
        val cookie = new DefaultCookie(Config.xitrum.session.cookieName, "0")
        cookie.setHttpOnly(true)
        cookie.setMaxAge(0)
        env.responseCookies.append(cookie)
      }
    } else {
      // See "restore" method
      // Convert to immutable because mutable cannot always be deserialized later!
      val immutableMap = session.toMap

      // Most browsers do not support cookie > 4KB
      val serialized = SeriDeseri.toSecureUrlSafeBase64(immutableMap, true)
      val cookieSize = serialized.length
      if (cookieSize > 4 * 1024) {
        Log.error("Cookie size = " + cookieSize + " > 4KB limit: " + immutableMap)
        return
      }

      val previousSessionCookieValueo = env.requestCookies.get(Config.xitrum.session.cookieName)
      if (previousSessionCookieValueo.isEmpty ||
          previousSessionCookieValueo.get != serialized ||
          Config.xitrum.session.cookieMaxAge > 0)  // Slide maxAge
      {
        // DefaultCookie has max age of Integer.MIN_VALUE by default,
        // which means the cookie will be removed when user terminates browser
        val cookie = new DefaultCookie(Config.xitrum.session.cookieName, serialized)
        cookie.setHttpOnly(true)
        cookie.setMaxAge(Config.xitrum.session.cookieMaxAge)
        env.responseCookies.append(cookie)
      }
    }
  }

  def restore(env: SessionEnv): Session = {
    env.requestCookies.get(Config.xitrum.session.cookieName) match {
      case None =>
        MMap.empty[String, Any]

      // See "store" method to know why this map needs to be immutable
      case Some(encryptedImmutableMap) =>
        val immutableMap = SeriDeseri
          .fromSecureUrlSafeBase64[Map[String, Any]](encryptedImmutableMap, true)
          .getOrElse(Map.empty[String, Any])

        // Convert to mutable map
        val ret = MMap.empty[String, Any]
        ret ++= immutableMap
        ret
    }
  }

}
