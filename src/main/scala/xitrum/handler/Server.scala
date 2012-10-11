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
    Routes.fromSockJsController()
    Routes.printRoutes()
    Routes.printActionPageCaches()
    Routes.printSockJsRoutes()

    if (Config.config.port.http.isDefined)  start(false)
    if (Config.config.port.https.isDefined) start(true)
    if (Config.config.port.flashSocketPolicy.isDefined) FlashSocketPolicyServer.start()

    val mode = if (Config.isProductionMode) "production" else "development"
    logger.info("Xitrum started in {} mode", mode)

    // This is a good timing to warn
    Config.warnOnDefaultSecureKey()
  }

  private def start(https: Boolean) {
    val channelFactory  = new NioServerSocketChannelFactory
    val bootstrap       = new ServerBootstrap(channelFactory)
    val pipelineFactory = new ChannelPipelineFactory(https)
    val (kind, port)    = if (https) ("HTTPS", Config.config.port.https.get) else ("HTTP", Config.config.port.http.get)

    bootstrap.setPipelineFactory(pipelineFactory)
    NetOption.set(bootstrap)
    try {
      bootstrap.bind(new InetSocketAddress(port))
      logger.info("{} server started on port {}", kind, port)
    } catch {
      case e =>
        val msg = "Could not open port for %s server. Check to see if there's another process running on port %d.".format(kind, port)
        Config.exitOnError(msg, e)
    }
  }
}
