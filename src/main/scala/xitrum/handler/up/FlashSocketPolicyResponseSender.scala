package xitrum.handler.up

import org.jboss.netty.buffer.ChannelBuffers
import org.jboss.netty.channel.{ChannelHandler, SimpleChannelUpstreamHandler, ChannelHandlerContext, MessageEvent}
import ChannelHandler.Sharable

import xitrum.util.Loader

object FlashSocketPolicyResponseSender {
  val RESPONSE = Loader.bytesFromClasspath("flash_socket_policy.xml")
}

@Sharable
class FlashSocketPolicyResponseSender extends SimpleChannelUpstreamHandler with BadClientSilencer {
  import FlashSocketPolicyResponseSender._

  override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {
    ctx.getChannel.write(ChannelBuffers.wrappedBuffer(RESPONSE))
  }
}
