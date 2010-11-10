package xt.vc.helper.session

import xt.Config
import xt.vc.Helper

import java.util.{UUID, HashMap => JMap}

import org.jboss.netty.handler.codec.http.DefaultCookie

object SessionRestorer {
  def restore(helper: Helper): Session = {
    val id = restoreSessionId(helper)
    val ret = Config.sessionStore.read(id) match {
      case Some(s) => s
      case None    => new Session(id, new JMap[String, Any](), Config.sessionStore)
    }

    // Store session ID to cookie
    SessionUtil.findSessionCookie(helper.cookies) match {
      case Some(cookie) =>
        cookie.setHttpOnly(true)
        cookie.setPath("/")
        cookie.setValue(ret.id)

      case None =>
        val cookie = new DefaultCookie(Config.sessionIdName, ret.id)
        cookie.setHttpOnly(true)
        cookie.setPath("/")
        helper.cookies.add(cookie)
    }

    ret
  }

  //----------------------------------------------------------------------------

  private def restoreSessionId(helper: Helper): String = {
    SessionUtil.findSessionCookie(helper.cookies) match {
      case Some(cookie) =>
        cookie.getValue

      case None =>
        // No cookie, try from the request parameters
        if (Config.sessionIdInCookieOnly) {
          UUID.randomUUID.toString
        } else {
          val xs = helper.allParams.get(Config.sessionIdName)
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
