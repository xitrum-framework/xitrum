package xitrum.handler

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelInitializer
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel

import xitrum.{Config, Log}
import xitrum.handler.inbound.FlashSocketPolicyHandler

object FlashSocketPolicyServer extends Log {
  def start() {
    val port        = Config.xitrum.port.flashSocketPolicy.get
    val bossGroup   = new NioEventLoopGroup
    val workerGroup = new NioEventLoopGroup
    val bootstrap   = new ServerBootstrap
    bootstrap.group(bossGroup, workerGroup)
             .channel(classOf[NioServerSocketChannel])
             .childHandler(new ChannelInitializer[SocketChannel] {
               override def initChannel(ch: SocketChannel) {
                 ch.pipeline.addLast(new FlashSocketPolicyHandler)
               }
             })

    NetOption.setOptions(bootstrap)
    NetOption.bind("flash socket", bootstrap, port)

    log.info("Flash socket policy server started on port {}", port)
  }
}
