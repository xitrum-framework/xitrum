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
    try {
      val cookie       = extEnv.cookies.get(Config.config.session.cookieName).get
      val base64String = cookie.getValue
      val sessionId    = SecureBase64.decrypt(base64String).get.asInstanceOf[String]

      // See "store" method
      val immutableMap = HazelcastSessionStore.store.get(sessionId)
      val ret          = new HazelcastSession(Some(sessionId))
      ret ++= immutableMap
      ret
    } catch {
      case _ =>
        // Cannot always get cookie, decrypt, deserialize, and type casting due to program changes etc.
        new HazelcastSession(None)
    }
  }

  def store(session: Session, extEnv: ExtEnv) {
    if (session.isEmpty) {
      extEnv.cookies.get(Config.config.session.cookieName) match {
        case None =>
        case Some(cookie) =>
          cookie.setMaxAge(0)

          val hSession = session.asInstanceOf[HazelcastSession]
          hSession.sessionId.foreach(HazelcastSessionStore.store.removeAsync(_))
      }
    } else {
      val hSession   = session.asInstanceOf[HazelcastSession]
      val hSessionId =
        hSession.sessionId match {
          case Some(sid) =>
            sid

          case None =>
            // Session cookie has not been created
            val sessionId  = UUID.randomUUID().toString
            val cookie     = new DefaultCookie(Config.config.session.cookieName, SecureBase64.encrypt(sessionId))
            val cookiePath = Config.withBaseUrl("/")
            cookie.setHttpOnly(true)
            cookie.setPath(cookiePath)
            extEnv.cookies.add(cookie)
            sessionId
        }

      // See "restore" method
      // Convert to immutable because mutable cannot always be deserialize later!
      val immutableMap = session.toMap
      HazelcastSessionStore.store.put(hSessionId, immutableMap)
    }
  }
}
