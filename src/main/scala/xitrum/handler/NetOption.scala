package xitrum.handler

import org.jboss.netty.bootstrap.ServerBootstrap

object NetOption {
  def set(bootstrap: ServerBootstrap) {
    // Backlog of 128 seems to be a sweet spot because that's OS default.
    // But that's still not optimal.
    // See http://lionet.livejournal.com/42016.html and the
    // "Tune Linux for many connections" section in the Xitrum guide
    bootstrap.setOption("backlog",          1024)
    bootstrap.setOption("reuseAddress",     true)
    bootstrap.setOption("child.tcpNoDelay", true)
    bootstrap.setOption("child.keepAlive",  true)
  }
}
