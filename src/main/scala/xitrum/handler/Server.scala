package xitrum.handler

import org.jboss.netty.bootstrap.ServerBootstrap
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory

import xitrum.{Cache, Config, Logger}
import xitrum.routing.Routes

object Server extends Logger {
  def start() {
    xitrum.util.SingleActorInstance.lookup("x")

    // Because Hazelcast takes serveral seconds to start, we force it to
    // start before the web server begins receiving requests, instead of
    // letting it start lazily
    Cache.cache.size()

    Routes.fromCacheFileOrRecollect()
    Routes.fromSockJsController()
    Routes.printRoutes()
    Routes.printActionPageCaches()
    Routes.printSockJsRoutes()

    val pc = Config.xitrum.port
    if (pc.http.isDefined)  start(false)
    if (pc.https.isDefined) start(true)
    if (pc.flashSocketPolicy.isDefined) FlashSocketPolicyServer.start()

    val mode = if (Config.productionMode) "production" else "development"
    logger.info("Xitrum started in {} mode", mode)

    // This is a good timing to warn
    Config.warnOnDefaultSecureKey()
  }

  private def start(https: Boolean) {
    val channelFactory  = new NioServerSocketChannelFactory
    val bootstrap       = new ServerBootstrap(channelFactory)
    val pipelineFactory = new ChannelPipelineFactory(https)
    val pc              = Config.xitrum.port
    val (service, port) = if (https) ("HTTPS", pc.https.get) else ("HTTP", pc.http.get)

    bootstrap.setPipelineFactory(pipelineFactory)
    NetOption.setOptions(bootstrap)
    NetOption.bind(service, bootstrap, port)
    logger.info("{} server started on port {}", service, port)
  }
}
