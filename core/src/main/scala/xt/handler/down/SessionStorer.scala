package xt.handler.down

import xt.Config
import xt.vc.Env
import xt.vc.session._

import org.jboss.netty.channel._
import org.jboss.netty.handler.codec.http.DefaultCookie

class SessionStorer extends ResponseHandler {
  def handleResponse(ctx: ChannelHandlerContext, e: MessageEvent, env: Env) {
    import env._

    if (session != null) {  // == null: SessionRestorer has not been run
      Config.sessionStore.write(session)

      // Remember session ID to cookie
      SessionUtil.findSessionCookie(cookies) match {
        case Some(cookie) =>
          cookie.setPath("/")
          cookie.setValue(env.session.id)

        case None =>
          val cookie = new DefaultCookie(Config.sessionIdName, env.session.id)
          cookie.setPath("/")
          cookies.add(cookie)
      }
    }

    Channels.write(ctx, e.getFuture, env)
  }
}
