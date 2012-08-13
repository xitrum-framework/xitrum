package xitrum.handler

import java.net.InetSocketAddress

import org.jboss.netty.channel.{Channels, ChannelPipeline, ChannelPipelineFactory => CPF}
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory
import org.jboss.netty.bootstrap.ServerBootstrap

import xitrum.{Config, Logger}
import xitrum.handler.up.FlashSocketPolicy

object FlashSocketPolicyServer extends Logger {
  def start() {
    val channelFactory  = new NioServerSocketChannelFactory
    val bootstrap       = new ServerBootstrap(channelFactory)
    val pipelineFactory = new FlashSocketChannelPipelineFactory

    bootstrap.setPipelineFactory(pipelineFactory)
    bootstrap.setOption("backlog",          128)  // 128 is a sweet spot, http://lionet.livejournal.com/42016.html
    bootstrap.setOption("reuseAddress",     true)
    bootstrap.setOption("child.tcpNoDelay", true)
    bootstrap.setOption("child.keepAlive",  true)

    val port = Config.config.port.flashSocketPolicy.get
    try {
      bootstrap.bind(new InetSocketAddress(port))
      logger.info("Flash socket policy server started on port {}", port)
    } catch {
      case e =>
        Config.exitOnError("Check to see if there's another process running on port " + port, e)
    }
  }
}

class FlashSocketChannelPipelineFactory extends CPF {
  private val flashSocketPolicy = new FlashSocketPolicy

  def getPipeline: ChannelPipeline = {
    Channels.pipeline(flashSocketPolicy)
  }
}
