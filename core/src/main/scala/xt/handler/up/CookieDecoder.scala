package xt.handler.up

import xt.handler._

import java.util.{UUID, HashMap => JMap, TreeSet}

import org.jboss.netty.channel._
import org.jboss.netty.handler.codec.http.{HttpHeaders, CookieDecoder => NCookieDecoder, Cookie}
import HttpHeaders.Names._

/** Decodes once so that the application does not have to decode again and again */
class CookieDecoder extends RequestHandler {
  def handleRequest(ctx: ChannelHandlerContext, env: XtEnv) {
    import env._

    val decoder = new NCookieDecoder
    val header  = request.getHeader(COOKIE)
    val cookies = if (header != null) decoder.decode(header) else new TreeSet[Cookie]()

    env.cookies = cookies
    Channels.fireMessageReceived(ctx, env)
  }
}
