package xt.handler.up

import xt.vc.Env

import java.util.{UUID, HashMap => JMap, TreeSet}

import org.jboss.netty.channel._
import org.jboss.netty.handler.codec.http.{HttpHeaders, CookieDecoder => NCookieDecoder, Cookie}
import HttpHeaders.Names._

/** Decodes once so that the application does not have to decode again and again */
class CookieDecoder extends RequestHandler {
  def handleRequest(ctx: ChannelHandlerContext, env: Env) {
    import env._

    val decoder = new NCookieDecoder
    val header  = request.getHeader(COOKIE)
    cookies = if (header != null) decoder.decode(header) else new TreeSet[Cookie]()

    Channels.fireMessageReceived(ctx, env)
  }
}
