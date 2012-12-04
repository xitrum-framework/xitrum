package xitrum.scope.session

import java.util.UUID
import scala.collection.mutable.HashMap

import org.jboss.netty.handler.codec.http.DefaultCookie
import com.hazelcast.core.IMap

import xitrum.Config
import xitrum.scope.request.ExtEnv
import xitrum.util.SecureBase64

/**
 * sessionId is needed to save the session to Hazelcast, None means browser did
 * not send session cookie.
 *
 * Subclass of HashMap => subclass of mutable Map = Session
 */
class HazelcastSession(val sessionId: Option[String]) extends HashMap[String, Any]

// We can use Cache, but we use a separate Hazelcast map to avoid the cost of
// iterating through a big map as much as we can. Another reason is that the
// application may need to config Hazelcast to persist sessions to a place
// (disk, DB etc.) different to those for other things (cache, comet etc.).
object HazelcastSessionStore {
  val store = Config.hazelcastInstance.getMap("xitrum/session").asInstanceOf[IMap[String, Map[String, Any]]]
}

class HazelcastSessionStore extends SessionStore {
  def restore(extEnv: ExtEnv): Session = {
    val sessionCookieName = Config.config.session.cookieName
    extEnv.cookies.get(sessionCookieName) match {
      case None =>
        new HazelcastSession(None)

      case Some(cookie) =>
        val base64String = cookie.getValue
        SecureBase64.decrypt(base64String) match {
          case None =>
            // sessionId sent by browser is invalid, recreate
            val sessionId  = UUID.randomUUID().toString
            cookie.setValue(SecureBase64.encrypt(sessionId))
            new HazelcastSession(Some(sessionId))

          case Some(any) =>
            val sessionIdo =
              try {
                Some(any.asInstanceOf[String])
              } catch {
                case _ => None
              }

            sessionIdo match {
              case None =>
                // sessionId sent by browser is not a String
                // (due to the switch from CookieSessionStore to HazelcastSessionStore etc.),
                // recreate
                val sessionId = UUID.randomUUID().toString
                cookie.setValue(SecureBase64.encrypt(sessionId))
                new HazelcastSession(Some(sessionId))

              case Some(sessionId) =>
                // See "store" method to know why this map is immutable
                // immutableMap can be null because Hazelcast does not have it
                val immutableMap = HazelcastSessionStore.store.get(sessionId)

                val ret = new HazelcastSession(Some(sessionId))
                if (immutableMap != null) ret ++= immutableMap
                ret
            }
        }
    }
  }

  def store(session: Session, extEnv: ExtEnv) {
    val sessionCookieName = Config.config.session.cookieName
    if (session.isEmpty) {
      // Remove session cookie
      extEnv.cookies.get(sessionCookieName) match {
        case None =>
          // Session cookie has not been sent by browser, no need to send anything back

        case Some(cookie) =>
          // Set max age to 0 so that browser will delete it immediately
          cookie.setMaxAge(0)

          val hSession = session.asInstanceOf[HazelcastSession]
          hSession.sessionId.foreach(HazelcastSessionStore.store.removeAsync(_))
      }
    } else {
      val hSession   = session.asInstanceOf[HazelcastSession]
      val hSessionId =
        // See "restore", None means browser did not send session cookie
        hSession.sessionId match {
          case Some(sid) =>
            sid

          case None =>
            // Session cookie has not been created, create it
            // DefaultCookie has max age of Integer.MIN_VALUE by default,
            // which means the cookie will be removed when user terminates browser
            val sessionId  = UUID.randomUUID().toString
            val cookie     = new DefaultCookie(sessionCookieName, SecureBase64.encrypt(sessionId))
            val cookiePath = Config.withBaseUrl("/")
            cookie.setPath(cookiePath)
            cookie.setHttpOnly(true)
            extEnv.cookies.add(cookie)
            sessionId
        }

      // See "restore" method
      // Convert to immutable because mutable cannot always be deserialized later!
      val immutableMap = session.toMap
      HazelcastSessionStore.store.put(hSessionId, immutableMap)
    }
  }
}
