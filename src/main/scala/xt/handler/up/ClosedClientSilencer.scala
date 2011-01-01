package xt.handler.up

import xt.Logger

import java.io.IOException
import java.nio.channels.ClosedChannelException
import org.jboss.netty.channel.{ChannelHandlerContext, ExceptionEvent, SimpleChannelUpstreamHandler}

/**
 * Closed clients will cause java.io.IOException and java.nio.channels.ClosedChannelException
 * when the server sends response. This trait will log such exceptions in
 * DEBUG instead of ERROR level.
 */
trait ClosedClientSilencer extends Logger {
  this: SimpleChannelUpstreamHandler =>

  override def exceptionCaught(ctx: ChannelHandlerContext, e: ExceptionEvent) {
    val cause = e.getCause
    if (cause.isInstanceOf[IOException] || cause.isInstanceOf[ClosedChannelException]) {
      logger.debug("ClosedClientSilencer", cause)
    } else {
      logger.error(getClass.getName, cause)
    }
    e.getChannel.close
  }
}
