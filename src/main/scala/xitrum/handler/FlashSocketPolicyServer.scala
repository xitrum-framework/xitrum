package xitrum.handler

import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory
import org.jboss.netty.bootstrap.ServerBootstrap

import xitrum.{Config, Log}

object FlashSocketPolicyServer extends Log {
  def start() {
//    val channelFactory  = new NioServerSocketChannelFactory
//    val bootstrap       = new ServerBootstrap(channelFactory)
//    val pipelineFactory = new FlashSocketChannelPipelineFactory
//    val port            = Config.xitrum.port.flashSocketPolicy.get
//
//    bootstrap.setPipelineFactory(pipelineFactory)
//    NetOption.setOptions(bootstrap)
//    NetOption.bind("flash socket", bootstrap, port)
//    log.info("Flash socket policy server started on port {}", port)
  }
}
