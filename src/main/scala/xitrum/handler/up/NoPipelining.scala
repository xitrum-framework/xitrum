package xitrum.handler.up

import io.netty.channel.{ChannelHandler, SimpleChannelUpstreamHandler, ChannelHandlerContext, MessageEvent}
import ChannelHandler.Sharable

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

    // Pause reading
    channel.setReadable(false)

    ctx.sendUpstream(e)
  }
}
