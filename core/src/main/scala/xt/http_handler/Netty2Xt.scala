package xt.http_handler

import xt._

import org.jboss.netty.channel._
import org.jboss.netty.handler.codec.http._
import HttpResponseStatus._
import HttpVersion._

class Netty2Xt extends SimpleChannelUpstreamHandler with Logger {
  override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {
    val m = e.getMessage
    if (!m.isInstanceOf[HttpRequest]) {
      ctx.sendUpstream(e)
      return
    }

    val request = m.asInstanceOf[HttpRequest]
    val env = new XtEnv
    env.request  = request
    env.response = new DefaultHttpResponse(HTTP_1_1, OK)
    Channels.fireMessageReceived(ctx, env)
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, e: ExceptionEvent) {
    logger.error("exceptionCaught", e.getCause)
  }
}
