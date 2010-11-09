package xt.handler.up

import xt.Logger
import xt.vc.Env

import org.jboss.netty.channel.{SimpleChannelUpstreamHandler, ChannelHandlerContext, MessageEvent, ExceptionEvent, ChannelFutureListener}
import org.jboss.netty.handler.codec.http.HttpHeaders

trait RequestHandler extends SimpleChannelUpstreamHandler with Logger {
  override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {
    val m = e.getMessage
    if (!m.isInstanceOf[Env]) {
      ctx.sendUpstream(e)
      return
    }

    val env = m.asInstanceOf[Env]
    handleRequest(ctx, env)
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, e: ExceptionEvent) {
    logger.error("exceptionCaught", e.getCause)
  }

  def handleRequest(ctx: ChannelHandlerContext, env: Env)

  protected def respond(ctx: ChannelHandlerContext, env: Env) {
    ctx.getChannel.write(env)
  }
}
