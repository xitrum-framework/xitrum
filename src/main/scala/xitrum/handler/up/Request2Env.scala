package xitrum.handler.up

import io.netty.channel.{ChannelHandler, SimpleChannelUpstreamHandler, ChannelHandlerContext, MessageEvent, ExceptionEvent, Channels}
import ChannelHandler.Sharable
import io.netty.handler.codec.http.{HttpHeaders, HttpRequest, DefaultHttpResponse, HttpResponseStatus, HttpVersion}

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
    env.channel = ctx.getChannel
    env.request = m.asInstanceOf[HttpRequest]
    env.response = {  /** The default response is empty 200 OK */
      // http://en.wikipedia.org/wiki/HTTP_persistent_connection
      // In HTTP 1.1 all connections are considered persistent unless declared otherwise
      val ret = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK)
      HttpHeaders.setContentLength(ret, 0)
      ret
    }

    Channels.fireMessageReceived(ctx, env)
  }
}
