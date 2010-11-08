package xt.handler.down

import xt.handler._

import java.util.{UUID, HashMap => JMap, TreeSet}

import org.jboss.netty.channel._
import org.jboss.netty.handler.codec.http.{HttpRequest, HttpResponse, HttpHeaders, CookieEncoder => NCookieEncoder, Cookie => NCookie}
import HttpHeaders.Names._

class CookieEncoder extends ResponseHandler {
  def handleResponse(ctx: ChannelHandlerContext, e: MessageEvent, env: XtEnv) {
    import env._

    if (cookies != null && cookies.size > 0) {   // == null: CookieDecoder has not been run
      val encoder = new NCookieEncoder(true)
      val iter = env.cookies.iterator
      while (iter.hasNext) encoder.addCookie(iter.next)
      response.setHeader(SET_COOKIE, encoder.encode)
    }

    Channels.write(ctx, e.getFuture, env)
  }
}
