package xitrum.handler

import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel.socket.SocketChannel
import io.netty.handler.ssl.{SslContextBuilder, SslHandler, SslProvider}

import xitrum.Config
import xitrum.handler.inbound.FlashSocketPolicyHandler

object SslChannelInitializer {
  val context = {
    val https    = Config.xitrum.https.get
    val provider = if (https.openSSL) SslProvider.OPENSSL else SslProvider.JDK
    SslContextBuilder.forServer(https.certChainFile, https.keyFile).sslProvider(provider).build()
  }
}

/** This is a wrapper. It prepends SSL handler to the non-SSL pipeline. */
@Sharable
class SslChannelInitializer(nonSslChannelInitializer: ChannelInitializer[SocketChannel]) extends ChannelInitializer[SocketChannel] {
  override def initChannel(ch: SocketChannel) {
    val p = ch.pipeline
    p.addLast(classOf[SslHandler].getName, SslChannelInitializer.context.newHandler(ch.alloc))
    p.addLast(nonSslChannelInitializer)

    // FlashSocketPolicyHandler can't be used with SSL
    DefaultHttpChannelInitializer.removeHandlerIfExists(p, classOf[FlashSocketPolicyHandler])
  }
}
