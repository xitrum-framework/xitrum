package xitrum

import java.io.File

import io.netty.channel.{ChannelInitializer, EventLoopGroup}
import io.netty.channel.socket.SocketChannel
import io.netty.util.ResourceLeakDetector

import xitrum.handler.{
  Bootstrap,
  DefaultHttpChannelInitializer,
  FlashSocketPolicyServer,
  NetOption,
  SslChannelInitializer
}

import xitrum.handler.inbound.Dispatcher
import xitrum.metrics.MetricsManager

object Server {

  private var eventLoopGroups = Seq.empty[EventLoopGroup]

  def stop() {
    eventLoopGroups.foreach(_.shutdownGracefully())
    Log.info(s"Xitrum $version stopped")
  }

  /**
   * Starts with the default ChannelInitializer provided by Xitrum.
   */
  def start(): Seq[EventLoopGroup] = {
    start(new DefaultHttpChannelInitializer)
  }

  /**
   * Starts with your custom ChannelInitializer. For an example, see
   * xitrum.handler.DefaultHttpChannelInitializer.
   * SSL codec handler will be automatically prepended for HTTPS server.
   */
  def start(httpChannelInitializer: ChannelInitializer[SocketChannel]): Seq[EventLoopGroup] = {
    Config.xitrum.loadExternalEngines()

    // Trick to start actorRegistry on startup
    xitrum.sockjs.SockJsAction.entropy()

    if (Config.xitrum.metrics.isDefined) MetricsManager.start()

    Config.routes.logAll()

    // By default, ResourceLeakDetector is enabled and at SIMPLE level
    // http://netty.io/4.0/api/io/netty/util/ResourceLeakDetector.Level.html
    if (!Config.productionMode) ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.ADVANCED)

    // Lastly, start the server(s) after necessary things have been prepared
    val portConfig = Config.xitrum.port
    if (portConfig.http.isDefined)  eventLoopGroups = eventLoopGroups ++ doStart(false, httpChannelInitializer)
    if (portConfig.https.isDefined) eventLoopGroups = eventLoopGroups ++ doStart(true,  httpChannelInitializer)

    // Flash socket server may use same port with HTTP server
    if (portConfig.flashSocketPolicy.isDefined) {
      if (portConfig.flashSocketPolicy != portConfig.http) {
        eventLoopGroups = eventLoopGroups ++ FlashSocketPolicyServer.start()
      } else {
        Log.info("Flash socket policy file will be served by the HTTP server")
      }
    }

    if (Config.productionMode)
      Log.info(s"Xitrum $version started in production mode")
    else
      Log.info(s"Xitrum $version started in development mode")

    // This is a good timing to warn
    Config.warnOnDefaultSecureKey()
    eventLoopGroups
  }

  //----------------------------------------------------------------------------

  private def doStart(https: Boolean, nonSslChannelInitializer: ChannelInitializer[SocketChannel]): Seq[EventLoopGroup] = {
    val channelInitializer =
      if (https)
        new SslChannelInitializer(nonSslChannelInitializer)
      else
        nonSslChannelInitializer

    val (bootstrap, groups) = Bootstrap.newBootstrap(channelInitializer)

    val portConfig      = Config.xitrum.port
    val (service, port) = if (https) ("HTTPS", portConfig.https.get) else ("HTTP", portConfig.http.get)

    NetOption.setOptions(bootstrap)
    NetOption.bind(service, bootstrap, port, groups)

    Config.xitrum.interface match {
      case None =>
        Log.info(s"$service server started on port $port")

      case Some(hostnameOrIp) =>
        Log.info(s"$service server started on $hostnameOrIp:$port")
    }
    groups
  }
}
