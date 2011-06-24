package xitrum

import java.net.InetSocketAddress
import java.util.concurrent.Executors

import org.jboss.netty.bootstrap.ServerBootstrap
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory

import xitrum.action.routing.Routes
import xitrum.handler.ChannelPipelineFactory

class Server extends Logger {
  def start {
    // Because Hazelcast takes serveral seconds to start, we force it to
    // start before the web server begin receiving requests, instead of
    // letting it start lazily
    Cache.cache.size

    Routes.collectAndCompile

    Runtime.getRuntime.availableProcessors * 2 + 1

    val bossExecutor   = Executors.newCachedThreadPool
    val workerExecutor = Executors.newCachedThreadPool
    val channelFactory = new NioServerSocketChannelFactory(bossExecutor, workerExecutor)

    val bootstrap = new ServerBootstrap(channelFactory)

    val pipelineFactory = new ChannelPipelineFactory
    bootstrap.setPipelineFactory(pipelineFactory)

    bootstrap.setOption("reuseAddress",     true)
    bootstrap.setOption("child.tcpNoDelay", true)
    bootstrap.setOption("child.keepAlive",  true)
    bootstrap.bind(new InetSocketAddress(Config.httpPort))

    val mode = if (Config.isProductionMode) "production" else "development"
    logger.info("Xitrum started on port {} in {} mode", Config.httpPort, mode)
  }
}
