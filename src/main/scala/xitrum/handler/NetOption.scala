package xitrum.handler

import java.net.InetSocketAddress

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.{ChannelFuture, ChannelFutureListener, ChannelOption}
import io.netty.channel.nio.NioEventLoopGroup

import xitrum.Config

object NetOption {
  def setOptions(bootstrap: ServerBootstrap) {
    // Backlog of 128 seems to be a sweet spot because that's OS default.
    // But that's still not optimal.
    // See http://lionet.livejournal.com/42016.html and the
    // "Tune Linux for many connections" section in the Xitrum guide
    bootstrap.option(ChannelOption.SO_BACKLOG,   Int.box(1024))
    bootstrap.option(ChannelOption.SO_REUSEADDR, Boolean.box(true))
    bootstrap.childOption(ChannelOption.TCP_NODELAY,  Boolean.box(true))
    bootstrap.childOption(ChannelOption.SO_KEEPALIVE, Boolean.box(true))
  }

  /** Stops the JVM process if cannot bind to the port. */
  def bind(service: String, bootstrap: ServerBootstrap, port: Int, bossGroup: NioEventLoopGroup, workerGroup: NioEventLoopGroup) {
    val ic = Config.xitrum.interface

    val addr = ic match {
      case None               => new InetSocketAddress(port)
      case Some(hostnameOrIp) => new InetSocketAddress(hostnameOrIp, port)
    }

    val portOpenFuture = bootstrap.bind(addr)
    portOpenFuture.awaitUninterruptibly()  // Wait for the port to be opened

    // Handle java.net.BindException
    // https://github.com/netty/netty/issues/1864
    // https://github.com/mauricio/postgresql-async/issues/68
    if (!portOpenFuture.isSuccess) {
      val msg = ic match {
        case None =>
          ("Could not open port %d for %s server. " +
          "Check to see if there's another process already running on that port.")
          .format(port, service)
        case Some(hostnameOrIp) =>
          ("Could not open %s:%d for %s server. " +
          "Check to see if there's another process already running at that address.")
          .format(hostnameOrIp, port, service)
      }

      bossGroup.shutdownGracefully()
      workerGroup.shutdownGracefully()
      Config.exitOnStartupError(msg, portOpenFuture.cause)
    }

    // Connection established successfully

    // Graceful shutdown when the socket is closed
    portOpenFuture.channel.closeFuture.addListener(new ChannelFutureListener {
      override def operationComplete(future: ChannelFuture) {
        bossGroup.shutdownGracefully()
        workerGroup.shutdownGracefully()
      }
    })
  }
}
