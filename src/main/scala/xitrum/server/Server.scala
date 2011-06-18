package xitrum.server

import java.net.InetSocketAddress
import java.util.concurrent.Executors

import org.jboss.netty.bootstrap.ServerBootstrap
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory

import xitrum.{Cache, Config, Logger}
import xitrum.action.routing.Routes

class Server extends Logger {
  def start {
    // TODO: remove this when this problem is solved
    // http://groups.google.com/group/simple-build-tool/browse_thread/thread/75a3d90e382a8b94
    System.setProperty("logback.configurationFile", "config/logback.xml")

    // Because Hazelcast takes serveral seconds to start, we force it to
    // start before the web server begin receiving requests, instead of
    // letting it start lazily
    Cache.cache.size

    Routes.collectAndCompile

    val pipelineFactory = new ChannelPipelineFactory
    val bootstrap =
      new ServerBootstrap(new NioServerSocketChannelFactory(
        Executors.newCachedThreadPool, Executors.newCachedThreadPool))
    bootstrap.setPipelineFactory(pipelineFactory)
    bootstrap.setOption("reuseAddress",     true)
    bootstrap.setOption("child.tcpNoDelay", true)
    bootstrap.setOption("child.keepAlive",  true)
    bootstrap.bind(new InetSocketAddress(Config.httpPort))

    val mode = if (Config.isProductionMode) "production" else "development"
    logger.info("Xitrum started on port {} in {} mode", Config.httpPort, mode)
  }
}
