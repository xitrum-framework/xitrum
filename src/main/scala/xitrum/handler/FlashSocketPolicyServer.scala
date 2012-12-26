package xitrum.handler

import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory
import org.jboss.netty.bootstrap.ServerBootstrap

import xitrum.{Config, Logger}

object FlashSocketPolicyServer extends Logger {
  def start() {
    val channelFactory  = new NioServerSocketChannelFactory
    val bootstrap       = new ServerBootstrap(channelFactory)
    val pipelineFactory = new FlashSocketChannelPipelineFactory
    val port            = Config.config.port.flashSocketPolicy.get

    bootstrap.setPipelineFactory(pipelineFactory)
    NetOption.setOptions(bootstrap)
    NetOption.bind("flash socket", bootstrap, port)
    logger.info("Flash socket policy server started on port {}", port)
  }
}
