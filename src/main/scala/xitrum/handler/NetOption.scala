package xitrum.handler

import java.net.InetSocketAddress

import io.netty.bootstrap.ServerBootstrap
import io.netty.buffer.PooledByteBufAllocator
import io.netty.channel.{ChannelFuture, ChannelFutureListener, ChannelOption, EventLoopGroup}

import xitrum.Config

object NetOption {
  /** Stops the JVM process if cannot bind to the port. */
  def bind(service: String, bootstrap: ServerBootstrap, port: Int, groups: Seq[EventLoopGroup]) {
    setOptions(bootstrap)

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

      groups.foreach(_.shutdownGracefully())
      Config.exitOnStartupError(msg, portOpenFuture.cause)
      return
    }

    // At this point, connection has been established successfully

    // Graceful shutdown when the socket is closed
    portOpenFuture.channel.closeFuture.addListener(new ChannelFutureListener {
      override def operationComplete(future: ChannelFuture) {
        groups.foreach(_.shutdownGracefully())
      }
    })
  }

  private def setOptions(bootstrap: ServerBootstrap) {
    // Backlog: Netty 4.0.23+ uses io.netty.util.NetUtil.SOMAXCONN for backlog by default.
    // See http://lionet.livejournal.com/42016.html and the
    // "Tune Linux for many connections" section in the Xitrum guide to know
    // how to tune the value for io.netty.util.NetUtil.SOMAXCONN to get.
    bootstrap.option(ChannelOption.SO_REUSEADDR, java.lang.Boolean.TRUE)
    bootstrap.childOption(ChannelOption.TCP_NODELAY,  java.lang.Boolean.TRUE)
    bootstrap.childOption(ChannelOption.SO_KEEPALIVE, java.lang.Boolean.TRUE)
    // https://github.com/netty/netty/issues/3859
    bootstrap.childOption(ChannelOption.ALLOCATOR,    PooledByteBufAllocator.DEFAULT)
  }
}
