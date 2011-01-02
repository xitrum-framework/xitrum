package xt.handler.down

import org.jboss.netty.channel.{ChannelHandler, SimpleChannelDownstreamHandler, ChannelHandlerContext, MessageEvent, Channels, ChannelFutureListener}
import ChannelHandler.Sharable
import org.jboss.netty.handler.codec.http.{HttpHeaders, HttpRequest, HttpResponse}

import xt.handler.Env

@Sharable
class Env2Response extends SimpleChannelDownstreamHandler {
  override def writeRequested(ctx: ChannelHandlerContext, e: MessageEvent) {
    val m = e.getMessage
    if (!m.isInstanceOf[Env]) {
      ctx.sendDownstream(e)
      return
    }

    val env      = m.asInstanceOf[Env]
    val request  = env("request").asInstanceOf[HttpRequest]
    val response = env("response").asInstanceOf[HttpResponse]
    val future   = e.getFuture
    Channels.write(ctx, future, response)

    // Keep alive
    //
    // If HttpHeaders.getContentLength(response) > response.getContent.readableBytes,
    // it is because the response body will be sent later and the channel will
    // be closed later by the code that sends the response body
    if (!HttpHeaders.isKeepAlive(request) && HttpHeaders.getContentLength(response) == response.getContent.readableBytes) {
      future.addListener(ChannelFutureListener.CLOSE)
    }
  }
}
