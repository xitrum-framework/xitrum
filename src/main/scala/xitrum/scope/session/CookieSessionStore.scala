package xitrum.scope.session

import scala.collection.mutable.{Map => MMap}

import org.jboss.netty.handler.codec.http.DefaultCookie

import xitrum.{Action, Config}
import xitrum.scope.request.ExtEnv
import xitrum.util.SecureBase64

class CookieSessionStore extends SessionStore {
  def restore(extEnv: ExtEnv): Session = {
    try {
      val cookie = extEnv.cookies.get(Config.config.session.cookieName).get
      val base64String = cookie.getValue
      val value = SecureBase64.decrypt(base64String).get

      // See "store" method below
      val immutableMap = value.asInstanceOf[Map[String, Any]]
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
    // See "restore" method above
    // Convert to immutable because mutable cannot always be deserialize later!
    val immutableMap = session.toMap

    val s = SecureBase64.encrypt(immutableMap)
    val cookiePath = Config.withBaseUri("/")
    extEnv.cookies.get(Config.config.session.cookieName) match {
      case Some(cookie) =>
        cookie.setHttpOnly(true)
        cookie.setPath(cookiePath)
        cookie.setValue(s)

      case None =>
        val cookie = new DefaultCookie(Config.config.session.cookieName, s)
        cookie.setHttpOnly(true)
        cookie.setPath(cookiePath)
        extEnv.cookies.add(cookie)
    }
  }
}
