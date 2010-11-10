package xt.vc.env

import xt.Config
import xt.vc.Env
import xt.vc.session._

import java.util.{UUID, HashMap => JMap}

object SessionRestorer {
  def restore(env: Env): Session = {
    val id = restoreSessionId(env)
    Config.sessionStore.read(id) match {
      case Some(s) => s
      case None    => new Session(id, new JMap[String, Any](), Config.sessionStore)
    }
  }

  //----------------------------------------------------------------------------

  private def restoreSessionId(env: Env): String = {
    SessionUtil.findSessionCookie(env.cookies) match {
      case Some(cookie) =>
        cookie.getValue

      case None =>
        // No cookie, try from the request parameters
        if (Config.sessionIdInCookieOnly) {
          UUID.randomUUID.toString
        } else {
          val xs = env.allParams.get(Config.sessionIdName)
          if (xs == null) {
            UUID.randomUUID.toString
          } else {
            val sid = xs.get(0)
            checkSessionIdFromRequestParameters(sid)
          }
        }
    }
  }

  /**
   * Session ID from request parameters is prone to session fixation
   * => Avoid this problem by looking up the session store and create
   * a new ID if the store does not contain the ID
   */
  private def checkSessionIdFromRequestParameters(id: String): String = {
    Config.sessionStore.read(id) match {
      case None => UUID.randomUUID.toString
      case _    => id
    }
  }
}
