package xitrum.handler

import io.netty.channel.{Channel, ChannelFuture, ChannelFutureListener}
import io.netty.handler.codec.http.{HttpHeaders, HttpRequest}

/**
 * http://en.wikipedia.org/wiki/HTTP_pipelining
 *
 * Xitrum does not support real pipelining. A client may send multiple requests,
 * but Xitrum will not process them at concurrently. Xitrum will process one
 * by one.
 *
 * From Mongrel2 doc: http://mongrel2.org/manual/book-finalch6.html
 *
 * "Where problems come in is with pipe-lined requests, meaning a browser sends
 * a bunch of requests in a big blast, then hangs out for all the responses.
 * This was such a horrible stupid idea that pretty much everone gets it wrong
 * and doesn't support it fully, if at all. The reason is it's much too easy to
 * blast a server with a ton of request, wait a bit so they hit proxied backends,
 * and then close the socket. The web server and the backends are now screwed
 * having to handle these requests which will go nowhere."
 */
object NoRealPipelining {
  def pauseReading(channel: Channel) {
    channel.config.setAutoRead(false)
  }

  def resumeReading(channel: Channel) {
    // We don't have to call channel.read() because setAutoRead also calls
    // channel.read() if not reading
    channel.config.setAutoRead(true)
  }

  /**
   * Handle keep alive as long as there's the request contains
   * 'connection:Keep-Alive' header, no matter what the client is 1.0 or 1.1:
   * http://sockjs.github.com/sockjs-protocol/sockjs-protocol-0.3.3.html#section-157
   */
  def if_keepAliveRequest_then_resumeReading_else_closeOnComplete(
    request: HttpRequest, channel: Channel, channelFuture: ChannelFuture
  ) {
    if (HttpHeaders.isKeepAlive(request)) {
      channelFuture.addListener(new ChannelFutureListener {
        def operationComplete(future: ChannelFuture) { resumeReading(channel) }
      })
    } else {
      channelFuture.addListener(ChannelFutureListener.CLOSE)
    }
  }
}
