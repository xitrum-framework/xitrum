package xt.vc.env.session

import org.jboss.netty.handler.codec.http.DefaultCookie
import xt.{Action, Config}
import xt.vc.env.ExtEnv

class CookieSessionStore extends SessionStore {
  def restore(extEnv: ExtEnv): Session = {
    extEnv.cookies(Config.sessionMarker) match {
      case Some(cookie) =>
        val base64String = cookie.getValue
        val ret          = new CookieSession
        ret.deserialize(base64String)
        ret

      case None =>
        new CookieSession
    }
  }

  def store(session: Session, extEnv: ExtEnv) {
    val cookieSession = session.asInstanceOf[CookieSession]
    val s = cookieSession.serialize

    extEnv.cookies(Config.sessionMarker) match {
      case Some(cookie) =>
        cookie.setHttpOnly(true)
        cookie.setPath("/")
        cookie.setValue(s)

      case None =>
        val cookie = new DefaultCookie(Config.sessionMarker, s)
        cookie.setHttpOnly(true)
        cookie.setPath("/")
        extEnv.cookies.add(cookie)
    }
  }
}
