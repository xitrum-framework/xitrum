package xitrum.scope.session

import java.util.UUID
import scala.collection.mutable.HashMap

import org.jboss.netty.handler.codec.http.DefaultCookie
import com.hazelcast.core.IMap

import xitrum.Config
import xitrum.scope.request.ExtEnv
import xitrum.util.SecureUrlSafeBase64

/**
 * @param sessionId needed to save the session to Hazelcast
 *
 * @param newlyCreated false means sessionId is taken from valid session cookie
 * sent by browser, true means browser did not send session cookie or did send
 * but the cookie value is not a valid encrypted session ID
 *
 * Subclass of HashMap => subclass of mutable Map = Session
 */
class HazelcastSession(val sessionId: String, val newlyCreated: Boolean) extends HashMap[String, Any]

// We can use Cache, but we use a separate Hazelcast map to avoid the cost of
// iterating through a big map as much as we can. Another reason is that the
// application may need to config Hazelcast to persist sessions to a place
// (disk, DB etc.) different to those for other things (cache, comet etc.).
object HazelcastSessionStore {
  val store = Config.hazelcastInstance.getMap("xitrum/session").asInstanceOf[IMap[String, Map[String, Any]]]
}

class HazelcastSessionStore extends SessionStore {
  def restore(extEnv: ExtEnv): Session = {
    val sessionCookieName = Config.xitrum.session.cookieName
    extEnv.requestCookies.get(sessionCookieName) match {
      case None =>
        val sessionId = UUID.randomUUID().toString
        new HazelcastSession(sessionId, true)

      case Some(encryptedSessionId) =>
        SecureUrlSafeBase64.decrypt(encryptedSessionId) match {
          case None =>
            // sessionId sent by browser is invalid, recreate
            val sessionId = UUID.randomUUID().toString
            new HazelcastSession(sessionId, true)

          case Some(any) =>
            val sessionIdo =
              try {
                Some(any.asInstanceOf[String])
              } catch {
                case scala.util.control.NonFatal(e) => None
              }

            sessionIdo match {
              case None =>
                // sessionId sent by browser is not a String
                // (due to the switch from CookieSessionStore to HazelcastSessionStore etc.),
                // recreate
                val sessionId = UUID.randomUUID().toString
                new HazelcastSession(sessionId, true)

              case Some(sessionId) =>
                // See "store" method to know why this map is immutable
                // immutableMap can be null because Hazelcast does not have it
                val immutableMap = HazelcastSessionStore.store.get(sessionId)

                val ret = new HazelcastSession(sessionId, false)
                if (immutableMap != null) ret ++= immutableMap
                ret
            }
        }
    }
  }

  /** @param session has been restored by "restore" method */
  def store(session: Session, extEnv: ExtEnv) {
    val sessionCookieName = Config.xitrum.session.cookieName
    if (session.isEmpty) {
      // If session cookie has been sent by browser, send back session cookie
      // with max age = 0 so that browser will delete it immediately
      if (extEnv.requestCookies.isDefinedAt(sessionCookieName)) {
        val cookie = new DefaultCookie(sessionCookieName, "0")
        cookie.setHttpOnly(true)
        cookie.setMaxAge(0)
        extEnv.responseCookies.append(cookie)

        // Remove session in Hazelcast if any
        val hSession = session.asInstanceOf[HazelcastSession]
        if (!hSession.newlyCreated) HazelcastSessionStore.store.removeAsync(hSession.sessionId)
      }
    } else {
      val hSession = session.asInstanceOf[HazelcastSession]
      // newlyCreated: true means browser did not send session cookie or did send
      // but the cookie value is not a valid encrypted session ID
      if (hSession.newlyCreated) {
        // DefaultCookie has max age of Integer.MIN_VALUE by default,
        // which means the cookie will be removed when user terminates browser
        val cookie = new DefaultCookie(sessionCookieName, SecureUrlSafeBase64.encrypt(hSession.sessionId))
        cookie.setHttpOnly(true)
        extEnv.responseCookies.append(cookie)
      }

      // See "restore" method
      // Convert to immutable because mutable cannot always be deserialized later!
      val immutableMap = session.toMap
      HazelcastSessionStore.store.put(hSession.sessionId, immutableMap)
    }
  }
}
