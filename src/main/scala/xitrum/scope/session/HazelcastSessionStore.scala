package xitrum.scope.session

import java.util.UUID
import scala.collection.mutable.{Map => MMap}

import org.jboss.netty.handler.codec.http.DefaultCookie
import com.hazelcast.core.IMap

import xitrum.Config
import xitrum.scope.request.ExtEnv
import xitrum.util.SecureBase64

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
      val cookie = extEnv.cookies.get(Config.config.session.cookieName).get
      val base64String = cookie.getValue
      val sessionId = SecureBase64.decrypt(base64String).get

      // See "store" method below
      val immutableMap = HazelcastSessionStore.store.get(sessionId)
      val ret = MMap[String, Any]()
      ret ++= immutableMap
      ret
    } catch {
      case _ =>
        // Cannot always get cookie, decrypt, deserialize, and type casting due to program changes etc.
        MMap[String, Any]()
    }
  }

  def store(session: Session, extEnv: ExtEnv) {
    val cookiePath = Config.withBaseUri("/")

    val sessionId = extEnv.cookies.get(Config.config.session.cookieName) match {
      case Some(cookie) =>
        val ret = try {
          val base64String = cookie.getValue
          SecureBase64.decrypt(base64String).get.asInstanceOf[String]
        } catch {
          case _ =>
            UUID.randomUUID.toString
        }
        cookie.setHttpOnly(true)
        cookie.setPath(cookiePath)
        cookie.setValue(SecureBase64.encrypt(ret))
        ret

      case None =>
        val ret = UUID.randomUUID.toString
        val cookie = new DefaultCookie(Config.config.session.cookieName, SecureBase64.encrypt(ret))
        cookie.setHttpOnly(true)
        cookie.setPath(cookiePath)
        extEnv.cookies.add(cookie)
        ret
    }

    // See "restore" method above
    // Convert to immutable because mutable cannot always be deserialize later!
    val immutableMap = session.toMap
    HazelcastSessionStore.store.put(sessionId, immutableMap)
  }
}
