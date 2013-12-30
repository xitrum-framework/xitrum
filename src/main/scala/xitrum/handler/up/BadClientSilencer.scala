package xitrum.handler.up

import io.netty.channel.{ChannelHandlerContext, SimpleChannelInboundHandler}
import xitrum.Log

/**
 * Bad client = closed connection, malformed request etc.
 *
 * Do nothing if the exception is one of the following:
 * java.io.IOException: Connection reset by peer
 * java.io.IOException: Broken pipe
 * java.nio.channels.ClosedChannelException: null
 * javax.net.ssl.SSLException: not an SSL/TLS record (Use http://... URL to connect to HTTPS server)
 * java.lang.IllegalArgumentException: empty text (Use https://... URL to connect to HTTP server)
 */
trait BadClientSilencer extends Log {
  this: SimpleChannelInboundHandler[_ <: AnyRef] =>

  override def exceptionCaught(ctx: ChannelHandlerContext, e: Throwable) {
    ctx.close()

    val throwable = e.getCause
    val s         = throwable.toString
    if (!s.startsWith("scala.runtime.NonLocalReturnControl") &&
        !s.startsWith("java.nio.channels.ClosedChannelException") &&
        !s.startsWith("java.io.IOException") &&
        !s.startsWith("javax.net.ssl.SSLException") &&
        !s.startsWith("java.lang.IllegalArgumentException"))
      log.debug(getClass.getName + " -> BadClientSilencer", throwable)
  }
}
