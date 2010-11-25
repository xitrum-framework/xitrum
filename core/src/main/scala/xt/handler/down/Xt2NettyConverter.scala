package xt.handler.down

import xt.vc.Controller

import org.jboss.netty.channel.{ChannelHandler, SimpleChannelDownstreamHandler, ChannelHandlerContext, MessageEvent, Channels, ChannelFutureListener}
import ChannelHandler.Sharable
import org.jboss.netty.handler.codec.http.HttpHeaders

@Sharable
class Xt2NettyConverter extends SimpleChannelDownstreamHandler {
  override def writeRequested(ctx: ChannelHandlerContext, e: MessageEvent) {
    val m = e.getMessage
    if (!m.isInstanceOf[Controller]) {
      ctx.sendDownstream(e)
      return
    }

    val controller = m.asInstanceOf[Controller]
    val future     = e.getFuture
    Channels.write(ctx, future, controller.response)

    // Keep alive
    if (!HttpHeaders.isKeepAlive(controller.request)) {
      future.addListener(ChannelFutureListener.CLOSE)
    }
  }
}
