package xitrum.handler.inbound

import io.netty.channel.{ChannelHandlerContext, ChannelInboundHandler}
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
  this: ChannelInboundHandler =>

  override def exceptionCaught(ctx: ChannelHandlerContext, e: Throwable) {
    if (ctx.channel.isOpen) ctx.close()

    if (!e.isInstanceOf[java.io.IOException] &&
        !e.isInstanceOf[java.lang.IllegalArgumentException] &&
        !e.isInstanceOf[java.nio.channels.ClosedChannelException] &&
        !e.isInstanceOf[javax.net.ssl.SSLException] &&
        !e.isInstanceOf[scala.runtime.NonLocalReturnControl[_]] &&
        !e.isInstanceOf[io.netty.handler.ssl.NotSslRecordException])
      log.debug(getClass.getName + " -> BadClientSilencer", e)
  }
}
