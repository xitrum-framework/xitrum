package xitrum.handler.up

import org.jboss.netty.channel.{Channel, ChannelHandler, ChannelFuture, ChannelFutureListener, ChannelHandlerContext, MessageEvent, SimpleChannelUpstreamHandler}
import org.jboss.netty.handler.codec.http.{HttpHeaders, HttpRequest, HttpResponse, HttpVersion}
import ChannelHandler.Sharable

object NoPipelining {
  def pauseReading(channel: Channel) {
    channel.setReadable(false)
  }

  // https://github.com/veebs/netty/commit/64f529945282e41eb475952fde382f234da8eec7
  def setResponseHeaderForKeepAliveRequest(request: HttpRequest, response: HttpResponse) {
    if (HttpHeaders.isKeepAlive(request))
      response.setHeader(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE)
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
        def operationComplete(future: ChannelFuture) { channel.setReadable(true) }
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
      response.setHeader(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE)
      channelFuture.addListener(new ChannelFutureListener() {
        def operationComplete(future: ChannelFuture) { channel.setReadable(true) }
      })
    } else {
      channelFuture.addListener(ChannelFutureListener.CLOSE)
    }
  }
}

@Sharable
/** http://mongrel2.org/static/book-finalch6.html */
class NoPipelining extends SimpleChannelUpstreamHandler with BadClientSilencer {
  override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {
    val channel = ctx.getChannel

    // Just in case more than one request has been read in
    // https://github.com/netty/netty/issues/214
    if (!channel.isReadable) {
      channel.close()
      return
    }

    val m = e.getMessage
    if (!m.isInstanceOf[HttpRequest]) {
      ctx.sendUpstream(e)
      return
    }

    // See ChannelPipelineFactory
    // This is the first Xitrum handler, log the request
    if (logger.isTraceEnabled) logger.trace(m.asInstanceOf[HttpRequest].toString)

    NoPipelining.pauseReading(channel)
    ctx.sendUpstream(e)
  }
}
