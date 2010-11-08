package xt.handler.up

import xt._
import xt.handler._
import xt.vc.session._

import java.util.{UUID, HashMap => JMap}

import org.jboss.netty.channel._

class SessionRestorer extends RequestHandler {
  def handleRequest(ctx: ChannelHandlerContext, env: XtEnv) {
    import env._

    val id = restoreSessionId(env)
    session = Config.sessionStore.read(id) match {
      case Some(s) => s
      case None    => new Session(id, new JMap[String, Any](), Config.sessionStore)
    }

    Channels.fireMessageReceived(ctx, env)
  }

  //----------------------------------------------------------------------------

  private def restoreSessionId(env: XtEnv): String = {
    SessionUtil.findSessionCookie(env.cookies) match {
      case Some(cookie) =>
        cookie.getValue

      case None =>
        // No cookie, try from the request parameters
        val xs = env.params.get(Config.sessionIdName)
        if (xs == null)
          UUID.randomUUID.toString
        else {
           val sid = xs.get(0)
           checkSessionId(sid)
        }
    }
  }

  /**
   * Session ID from request parameters is prone to session fixation
   * => Avoid this problem by looking up the session store and create
   * a new ID if the store does not contain the ID
   */
  private def checkSessionId(id: String): String = {
    Config.sessionStore.read(id) match {
      case None => UUID.randomUUID.toString
      case _    => id
    }
  }
}
