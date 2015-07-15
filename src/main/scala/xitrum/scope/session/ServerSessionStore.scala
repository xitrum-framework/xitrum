package xitrum.scope.session

import java.util.UUID
import scala.collection.mutable.HashMap

import io.netty.handler.codec.http.cookie.DefaultCookie

import xitrum.Config
import xitrum.util.SeriDeseri

/**
 * @param sessionId needed to save the session to Cleakka
 *
 * @param newlyCreated false means sessionId is taken from valid session cookie
 * sent by browser, true means browser did not send session cookie or did send
 * but the cookie value is not a valid encrypted session ID
 *
 * Subclass of HashMap => subclass of mutable Map = Session
 */
class ServerSession(val sessionId: String, val newlyCreated: Boolean) extends HashMap[String, Any]

/**
 * For convenience, server side session store implementations should base on
 * this trait. It handles storing and restoring session ID in cookie for you.
 */
trait ServerSessionStore extends SessionStore {
  /** To be implemented by server side session store implementations */
  def get(sessionId: String): Option[Map[String, Any]]

  /** To be implemented by server side session store implementations */
  def put(sessionId: String, immutableMap: Map[String, Any])

  /** To be implemented by server side session store implementations */
  def remove(sessionId: String)

  //----------------------------------------------------------------------------

  def store(session: Session, env: SessionEnv) {
    if (session.isEmpty) {
      // If session cookie has been sent by browser, send back session cookie
      // with max age = 0 so that browser will delete it immediately
      if (env.requestCookies.isDefinedAt(Config.xitrum.session.cookieName)) {
        val cookie = new DefaultCookie(Config.xitrum.session.cookieName, "0")
        cookie.setHttpOnly(true)
        cookie.setMaxAge(0)
        env.responseCookies.append(cookie)

        // Remove session in Cleakka if any
        val hSession = session.asInstanceOf[ServerSession]
        if (!hSession.newlyCreated) remove(hSession.sessionId)
      }
    } else {
      val hSession = session.asInstanceOf[ServerSession]
      // newlyCreated: true means browser did not send session cookie or did send
      // but the cookie value is not a valid encrypted session ID
      if (hSession.newlyCreated || Config.xitrum.session.cookieMaxAge > 0) {
        val cookie = new DefaultCookie(Config.xitrum.session.cookieName, SeriDeseri.toSecureUrlSafeBase64(hSession.sessionId))
        cookie.setHttpOnly(true)
        cookie.setMaxAge(Config.xitrum.session.cookieMaxAge)
        env.responseCookies.append(cookie)
      }

      // See "restore" method
      // Convert to immutable because mutable cannot always be deserialized later!
      val immutableMap = session.toMap
      put(hSession.sessionId, immutableMap)
    }
  }

  def restore(env: SessionEnv): Session = {
    env.requestCookies.get(Config.xitrum.session.cookieName) match {
      case None =>
        val sessionId = UUID.randomUUID().toString
        new ServerSession(sessionId, true)

      case Some(encryptedSessionId) =>
        SeriDeseri.fromSecureUrlSafeBase64[String](encryptedSessionId) match {
          case None =>
            // sessionId sent by browser is invalid (probably due to the switch
            // from CookieSessionStore to CleakkaSessionStore etc.), recreate
            val sessionId = UUID.randomUUID().toString
            new ServerSession(sessionId, true)

          case Some(sessionId) =>
            // See "store" method to know why this map is immutable
            val immutableMapo = get(sessionId)

            val ret = new ServerSession(sessionId, false)
            immutableMapo.foreach { ret ++= _ }
            ret
        }
    }
  }
}
