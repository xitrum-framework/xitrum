package xitrum.handler

import java.net.InetSocketAddress

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
    NetOption.set(bootstrap)
    try {
      bootstrap.bind(new InetSocketAddress(port))
      logger.info("Flash socket policy server started on port {}", port)
    } catch {
      case e =>
        val msg = "Could not open port for flash socket server. Check to see if there's another process running on port %d.".format(port)
        Config.exitOnError(msg, e)
    }
  }
}
