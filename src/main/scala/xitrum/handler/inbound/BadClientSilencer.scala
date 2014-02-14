package xitrum.handler.inbound

import io.netty.channel.{ChannelHandler, ChannelHandlerContext, SimpleChannelInboundHandler}
import ChannelHandler.Sharable
import xitrum.Log

/**
 * This handler should be put at the last position of the inbound pipeline to
 * catch all exception caused be bad client (closed connection, malformed request etc.).
 *
 * Do nothing if the exception is one of the following:
 * java.io.IOException: Connection reset by peer
 * java.io.IOException: Broken pipe
 * java.nio.channels.ClosedChannelException: null
 * javax.net.ssl.SSLException: not an SSL/TLS record (Use http://... URL to connect to HTTPS server)
 * java.lang.IllegalArgumentException: empty text (Use https://... URL to connect to HTTP server)
 */
@Sharable
class BadClientSilencer extends SimpleChannelInboundHandler[Any] with Log {
  override def channelRead0(ctx: ChannelHandlerContext, env: Any) {
    ctx.channel.close()
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, e: Throwable) {
    if (e.isInstanceOf[java.io.IOException] ||
        e.isInstanceOf[java.lang.IllegalArgumentException] ||
        e.isInstanceOf[java.nio.channels.ClosedChannelException] ||
        e.isInstanceOf[javax.net.ssl.SSLException] ||
        e.isInstanceOf[io.netty.handler.codec.DecoderException] ||
        e.isInstanceOf[io.netty.handler.ssl.NotSslRecordException])
    {
      if (log.isTraceEnabled) log.trace("exceptionCaught", e)
    } else {
      log.warn("exceptionCaught", e)
    }
  }
}
