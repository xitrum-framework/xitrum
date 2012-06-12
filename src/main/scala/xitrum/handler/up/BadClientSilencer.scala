package xitrum.handler.up

import org.jboss.netty.channel.{ChannelHandlerContext, ExceptionEvent, SimpleChannelUpstreamHandler}
import xitrum.Logger

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
trait BadClientSilencer extends Logger {
  this: SimpleChannelUpstreamHandler =>

  override def exceptionCaught(ctx: ChannelHandlerContext, e: ExceptionEvent) {
    ctx.getChannel.close()

    val cause = e.getCause
    val s     = cause.toString
    if (s.startsWith("java.nio.channels.ClosedChannelException") ||
        s.startsWith("java.io.IOException") ||
        s.startsWith("javax.net.ssl.SSLException") ||
        s.startsWith("java.lang.IllegalArgumentException")) return

    logger.debug(getClass.getName + " -> BadClientSilencer", cause)
  }
}
