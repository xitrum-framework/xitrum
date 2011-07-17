package xitrum.handler.up

import org.jboss.netty.channel.{ChannelHandler, SimpleChannelUpstreamHandler, ChannelHandlerContext, MessageEvent, ExceptionEvent, Channels}
import ChannelHandler.Sharable
import org.jboss.netty.handler.codec.http.HttpRequest

import xitrum.handler.HandlerEnv

@Sharable
class Request2Env extends SimpleChannelUpstreamHandler with BadClientSilencer {
  override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {
    val m = e.getMessage
    if (!m.isInstanceOf[HttpRequest]) {
      ctx.sendUpstream(e)
      return
    }

    val env = new HandlerEnv
    env.request = m.asInstanceOf[HttpRequest]
    Channels.fireMessageReceived(ctx, env)
  }
}
