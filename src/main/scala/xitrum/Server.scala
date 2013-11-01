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

object Server extends Log {
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
    // Don't know why this doesn't work if put above Config.actorSystem
    //
    // Redirect Akka log to SLF4J
    // http://doc.akka.io/docs/akka/2.0/scala/logging.html
    // http://stackoverflow.com/questions/16202501/how-can-i-override-a-typesafe-config-list-value-on-the-command-line
    System.setProperty("akka.loggers.0", "akka.event.slf4j.Slf4jLogger")

    Config.xitrum.templateEngine.foreach(_.start())
    Config.xitrum.cache.start()
    Config.xitrum.session.store.start()

    // Trick to start actorRegistry on startup
    SockJsAction.entropy()

    val routes = Config.routes
    routes.logRoutes(false)
    routes.sockJsRouteMap.logRoutes()
    routes.logErrorRoutes()
    routes.logRoutes(true)

    // Lastly, start the server(s) after necessary things have been prepared
    val portConfig = Config.xitrum.port
    if (portConfig.http.isDefined)  doStart(false, httpChannelPipelineFactory)
    if (portConfig.https.isDefined) doStart(true,  httpChannelPipelineFactory)
    if (portConfig.flashSocketPolicy.isDefined) FlashSocketPolicyServer.start()

    val mode = if (Config.productionMode) "production" else "development"
    log.info("Xitrum started in {} mode", mode)

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

    Config.xitrum.interface match {
      case None =>
        log.info(s"${service} server started on port ${port}")

      case Some(hostnameOrIp) =>
        log.info(s"${service} server started on ${hostnameOrIp}:${port}")
    }
  }
}
