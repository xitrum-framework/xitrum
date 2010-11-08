package xt.http_handler

import xt._

import org.jboss.netty.channel.{SimpleChannelUpstreamHandler, ChannelHandlerContext, MessageEvent, ExceptionEvent, ChannelFutureListener}
import org.jboss.netty.handler.codec.http.HttpHeaders

trait RequestHandler extends SimpleChannelUpstreamHandler with Logger {
  override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {
    val m = e.getMessage
    if (!m.isInstanceOf[XtEnv]) {
      ctx.sendUpstream(e)
      return
    }

    val env = m.asInstanceOf[XtEnv]
    logger.debug("handleRequest")
    handleRequest(ctx, env)
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, e: ExceptionEvent) {
    logger.error("exceptionCaught", e.getCause)
  }

  def handleRequest(ctx: ChannelHandlerContext, env: XtEnv)

  protected def respond(ctx: ChannelHandlerContext, env: XtEnv) {
    ctx.getChannel.write(env)
  }
}
