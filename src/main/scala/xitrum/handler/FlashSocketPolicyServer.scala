package xitrum.handler

import io.netty.channel.{ChannelInitializer, EventLoopGroup}
import io.netty.channel.socket.SocketChannel

import xitrum.{Config, Log}
import xitrum.handler.inbound.FlashSocketPolicyHandler

object FlashSocketPolicyServer {
  def start(): Seq[EventLoopGroup] = {
    val (bootstrap, groups) = Bootstrap.newBootstrap(newChannelInitializer())

    val port = Config.xitrum.port.flashSocketPolicy.get
    NetOption.setOptions(bootstrap)
    NetOption.bind("flash socket", bootstrap, port, groups)

    Log.info(s"Flash socket policy server started on port $port")
    groups
  }

  private def newChannelInitializer(): ChannelInitializer[SocketChannel] = {
    new ChannelInitializer[SocketChannel] {
      override def initChannel(ch: SocketChannel) {
        ch.pipeline.addLast(new FlashSocketPolicyHandler)
      }
    }
  }
}
