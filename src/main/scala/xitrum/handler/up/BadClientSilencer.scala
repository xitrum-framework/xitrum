package xitrum.handler.up

import java.io.IOException
import java.nio.channels.ClosedChannelException
import org.jboss.netty.channel.{ChannelHandlerContext, ExceptionEvent, SimpleChannelUpstreamHandler}

import xitrum.Logger

/** Bad client = closed connection or malformed request etc. */
trait BadClientSilencer extends Logger {
  this: SimpleChannelUpstreamHandler =>

  override def exceptionCaught(ctx: ChannelHandlerContext, e: ExceptionEvent) {
    val cause = e.getCause
    logger.debug(getClass.getName + " -> BadClientSilencer", cause)
    e.getChannel.close
  }
}
