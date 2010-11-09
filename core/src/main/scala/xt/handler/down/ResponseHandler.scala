package xt.handler.down

import xt.Logger
import xt.vc.Env

import org.jboss.netty.channel.{SimpleChannelDownstreamHandler, ChannelHandlerContext, MessageEvent, ExceptionEvent}
import org.jboss.netty.handler.codec.http.{HttpRequest, HttpResponse}

trait ResponseHandler extends SimpleChannelDownstreamHandler with Logger {
  override def writeRequested(ctx: ChannelHandlerContext, e: MessageEvent) {
    val m = e.getMessage
    if (!m.isInstanceOf[Env]) {
      ctx.sendDownstream(e)
      return
    }

    val env = m.asInstanceOf[Env]
    handleResponse(ctx, e, env)
  }

  def handleResponse(ctx: ChannelHandlerContext, e: MessageEvent, env: Env)
}
