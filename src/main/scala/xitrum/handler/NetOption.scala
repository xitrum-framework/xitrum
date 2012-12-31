package xitrum.handler

import java.net.InetSocketAddress
import org.jboss.netty.bootstrap.ServerBootstrap
import xitrum.Config

object NetOption {
  def setOptions(bootstrap: ServerBootstrap) {
    // Backlog of 128 seems to be a sweet spot because that's OS default.
    // But that's still not optimal.
    // See http://lionet.livejournal.com/42016.html and the
    // "Tune Linux for many connections" section in the Xitrum guide
    bootstrap.setOption("backlog",          1024)
    bootstrap.setOption("reuseAddress",     true)
    bootstrap.setOption("child.tcpNoDelay", true)
    bootstrap.setOption("child.keepAlive",  true)
  }

  /** Stops the JVM process if cannot bind to the port. */
  def bind(service: String, bootstrap: ServerBootstrap, port: Int) {
    val addr = Config.config.interface match {
      case None               => new InetSocketAddress(port)
      case Some(hostnameOrIp) => new InetSocketAddress(hostnameOrIp, port)
    }

    try {
      bootstrap.bind(addr)
    } catch {
      case scala.util.control.NonFatal(e) =>
        val msg = Config.config.interface match {
          case None =>
            ("Could not open port %d for %s server. " +
            "Check to see if there's another process already running on that port.")
            .format(port, service)
          case Some(hostnameOrIp) =>
            ("Could not open %s:%d for %s server. " +
            "Check to see if there's another process already running at that address.")
            .format(hostnameOrIp, port, service)
        }
        Config.exitOnError(msg, e)
    }
  }
}
