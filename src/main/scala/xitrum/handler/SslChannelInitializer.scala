package xitrum.handler

import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel.ChannelInitializer
import io.netty.channel.socket.SocketChannel
import io.netty.handler.ssl.SslHandler

import xitrum.handler.inbound.FlashSocketPolicyHandler

/** This is a wrapper. It prepends SSL handler to the non-SSL pipeline. */
@Sharable
class SslChannelInitializer(nonSslChannelInitializer: ChannelInitializer[SocketChannel]) extends ChannelInitializer[SocketChannel] {
  override def initChannel(ch: SocketChannel): Unit = {
    val p = ch.pipeline
    p.addLast(classOf[SslHandler].getName, RebuilableSslContext.newHandler(ch.alloc))
    p.addLast(nonSslChannelInitializer)

    // FlashSocketPolicyHandler can't be used with SSL
    DefaultHttpChannelInitializer.removeHandlerIfExists(p, classOf[FlashSocketPolicyHandler])
  }
}
