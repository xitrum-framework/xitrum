package xt.handler.up

import xt.Logger
import xt.vc.Env

import org.jboss.netty.channel._
import org.jboss.netty.handler.codec.http._
import HttpResponseStatus._
import HttpVersion._

class Netty2XtConverter extends SimpleChannelUpstreamHandler with Logger {
  override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {
    val m = e.getMessage
    if (!m.isInstanceOf[HttpRequest]) {
      ctx.sendUpstream(e)
      return
    }

    val request = m.asInstanceOf[HttpRequest]
    val env = new Env
    env.request  = request
    env.response = new DefaultHttpResponse(HTTP_1_1, OK)
    Channels.fireMessageReceived(ctx, env)
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, e: ExceptionEvent) {
    logger.error("exceptionCaught", e.getCause)
  }
}
