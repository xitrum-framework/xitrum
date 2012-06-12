package xitrum.handler

import java.net.InetSocketAddress

import org.jboss.netty.bootstrap.ServerBootstrap
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory

import xitrum.{Cache, Config, Logger}
import xitrum.routing.Routes

object Server extends Logger {
  def start() {
    // Because Hazelcast takes serveral seconds to start, we force it to
    // start before the web server begins receiving requests, instead of
    // letting it start lazily
    Cache.cache.size()

    Routes.fromCacheFileOrRecollect()
    Routes.printRoutes()
    Routes.printActionPageCaches()

    if (Config.config.http.isDefined)  start(false)
    if (Config.config.https.isDefined) start(true)

    val mode = if (Config.isProductionMode) "production" else "development"
    logger.info("Xitrum started in {} mode", mode)
  }

  private def start(https: Boolean) {
    val channelFactory  = new NioServerSocketChannelFactory
    val bootstrap       = new ServerBootstrap(channelFactory)
    val pipelineFactory = new ChannelPipelineFactory(https)
    val (kind, port)    = if (https) ("HTTPS", Config.config.https.get.port) else ("HTTP", Config.config.http.get.port)

    bootstrap.setPipelineFactory(pipelineFactory)
    bootstrap.setOption("backlog",          128)  // 128 is a sweet spot, http://lionet.livejournal.com/42016.html
    bootstrap.setOption("reuseAddress",     true)
    bootstrap.setOption("child.tcpNoDelay", true)
    bootstrap.setOption("child.keepAlive",  true)
    try {
      bootstrap.bind(new InetSocketAddress(port))
      logger.info("{} server started on port {}", kind, port)
    } catch {
      case e =>
        Config.exitOnError("Check to see if there's another process running on port " + port, e)
    }
  }
}
