package xitrum

import io.netty.channel.socket.SocketChannel
import io.netty.channel.{ChannelInitializer, EventLoopGroup}
import xitrum.handler.inbound.FlashSocketPolicyHandler
import xitrum.handler.{Bootstrap, NetOption}

object FlashSocketPolicyServer {
  def start(): Seq[EventLoopGroup] = {
    val (bootstrap, groups) = Bootstrap.newBootstrap(newChannelInitializer())

    val port = Config.xitrum.port.flashSocketPolicy.get
    NetOption.bind("Flash socket", bootstrap, port, groups)

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
