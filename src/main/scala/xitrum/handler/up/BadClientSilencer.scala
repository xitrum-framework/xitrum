package xitrum.handler.up

import org.jboss.netty.channel.{ChannelHandlerContext, ExceptionEvent, SimpleChannelUpstreamHandler}

import xitrum.Logger

/** Bad client = closed connection, malformed request etc. */
trait BadClientSilencer extends Logger {
  this: SimpleChannelUpstreamHandler =>

  override def exceptionCaught(ctx: ChannelHandlerContext, e: ExceptionEvent) {
    if (e.getChannel.isOpen) e.getChannel.close

    // Do nothing if the exception is one of the following:
    // java.io.IOException: Connection reset by peer
    // java.io.IOException: Broken pipe
    // java.nio.channels.ClosedChannelException: null
    val cause = e.getCause
    val s     = cause.toString
    if (s.startsWith("java.nio.channels.ClosedChannelException") || s.startsWith("java.io.IOException")) return

    logger.debug(getClass.getName + " -> BadClientSilencer", cause)
  }
}
