package xitrum

import org.jboss.netty.bootstrap.ServerBootstrap
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory
import org.jboss.netty.channel.ChannelPipelineFactory

import xitrum.handler.{
  DefaultHttpChannelPipelineFactory,
  FlashSocketPolicyServer,
  NetOption,
  SslChannelPipelineFactory
}
import xitrum.sockjs.SockJsAction

object Server extends Logger {
  /**
   * Starts with default ChannelPipelineFactory provided by Xitrum.
   */
  def start() {
    val default = new DefaultHttpChannelPipelineFactory
    start(default)
  }

  /**
   * Starts with your custom ChannelPipelineFactory. For an example, see
   * xitrum.handler.DefaultHttpChannelPipelineFactory.
   * SSL codec handler will be automatically prepended for HTTPS server.
   */
  def start(httpChannelPipelineFactory: ChannelPipelineFactory) {
    // Because Hazelcast takes serveral seconds to start, we force it to
    // start before the web server begins receiving requests, instead of
    // letting it start lazily
    Cache.cache.size()

    // Trick to start actorRegistry on startup
    SockJsAction.entropy()

    // templateEngine is lazy, force its initialization here
    Config.xitrum.templateEngine

    val routes = Config.routes
    routes.printRoutes()
    routes.sockJsRouteMap.print()
    routes.printErrorRoutes()

    // Lastly, start the server(s) after necessary things have been prepared
    val portConfig = Config.xitrum.port
    if (portConfig.http.isDefined)  doStart(false, httpChannelPipelineFactory)
    if (portConfig.https.isDefined) doStart(true,  httpChannelPipelineFactory)
    if (portConfig.flashSocketPolicy.isDefined) FlashSocketPolicyServer.start()

    val mode = if (Config.productionMode) "production" else "development"
    logger.info("Xitrum started in {} mode", mode)

    // This is a good timing to warn
    Config.warnOnDefaultSecureKey()
  }

  private def doStart(https: Boolean, httpChannelPipelineFactory: ChannelPipelineFactory) {
    val channelPipelineFactory =
      if (https)
        new SslChannelPipelineFactory(httpChannelPipelineFactory)
      else
        httpChannelPipelineFactory

    val bootstrap       = new ServerBootstrap(new NioServerSocketChannelFactory)
    val portConfig      = Config.xitrum.port
    val (service, port) = if (https) ("HTTPS", portConfig.https.get) else ("HTTP", portConfig.http.get)

    bootstrap.setPipelineFactory(channelPipelineFactory)
    NetOption.setOptions(bootstrap)
    NetOption.bind(service, bootstrap, port)
    logger.info("{} server started on port {}", service, port)
  }
}
