package xt.handler.down

import xt._
import xt.handler._

import org.jboss.netty.channel._
import org.jboss.netty.handler.codec.http._

class Xt2NettyHandler extends ResponseHandler {
  def handleResponse(ctx: ChannelHandlerContext, e: MessageEvent, env: XtEnv) {
    val future = e.getFuture
    Channels.write(ctx, future, env.response)

    // Keep alive
    if (!HttpHeaders.isKeepAlive(env.request)) {
      future.addListener(ChannelFutureListener.CLOSE)
    }
  }
}
