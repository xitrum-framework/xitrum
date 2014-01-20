package xitrum.handler.inbound

import io.netty.channel.{Channel, ChannelHandler, ChannelFuture, ChannelFutureListener, ChannelHandlerContext, SimpleChannelInboundHandler}
import io.netty.handler.codec.http.{HttpHeaders, HttpRequest, HttpResponse, HttpVersion}
import ChannelHandler.Sharable

import xitrum.handler.HandlerEnv

object NoPipelining {
  def pauseReading(channel: Channel) {
    channel.config.setAutoRead(false)
  }

  // https://github.com/veebs/netty/commit/64f529945282e41eb475952fde382f234da8eec7
  def setResponseHeaderForKeepAliveRequest(request: HttpRequest, response: HttpResponse) {
    if (HttpHeaders.isKeepAlive(request))
      HttpHeaders.setHeader(response, HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE)
  }

  /**
   * Handle keep alive as long as there's the request contains
   * 'connection:Keep-Alive' header, no matter what the client is 1.0 or 1.1:
   * http://sockjs.github.com/sockjs-protocol/sockjs-protocol-0.3.3.html#section-157
   */
  def if_keepAliveRequest_then_resumeReading_else_closeOnComplete(
      request: HttpRequest,
      channel: Channel, channelFuture: ChannelFuture) {
    if (HttpHeaders.isKeepAlive(request)) {
      channelFuture.addListener(new ChannelFutureListener() {
        def operationComplete(future: ChannelFuture) { channel.config.setAutoRead(true) }
      })
    } else {
      channelFuture.addListener(ChannelFutureListener.CLOSE)
    }
  }

  // Combo of the above 2 methods
  def setResponseHeaderAndResumeReadingForKeepAliveRequestOrCloseOnComplete(
      request: HttpRequest, response: HttpResponse,
      channel: Channel, channelFuture: ChannelFuture) {
    if (HttpHeaders.isKeepAlive(request)) {
      HttpHeaders.setHeader(response, HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE)
      channelFuture.addListener(new ChannelFutureListener() {
        def operationComplete(future: ChannelFuture) { channel.config.setAutoRead(true) }
      })
    } else {
      channelFuture.addListener(ChannelFutureListener.CLOSE)
    }
  }
}

@Sharable
/** http://mongrel2.org/static/book-finalch6.html */
class NoPipelining extends SimpleChannelInboundHandler[HandlerEnv] {
  override def channelRead0(ctx: ChannelHandlerContext, env: HandlerEnv) {
    val channel = ctx.channel
    NoPipelining.pauseReading(channel)
    ctx.fireChannelRead(env)
  }
}
