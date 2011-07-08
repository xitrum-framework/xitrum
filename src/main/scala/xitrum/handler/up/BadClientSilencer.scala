package xitrum.handler.up

import org.jboss.netty.channel.{ChannelHandlerContext, ExceptionEvent, SimpleChannelUpstreamHandler}

import xitrum.Logger

/** Bad client = closed connection, malformed request etc. */
trait BadClientSilencer extends Logger {
  this: SimpleChannelUpstreamHandler =>

  override def exceptionCaught(ctx: ChannelHandlerContext, e: ExceptionEvent) {
    if (e.getChannel.isOpen) e.getChannel.close

    val cause = e.getCause
    logger.debug(getClass.getName + " -> BadClientSilencer", cause)
  }
}
