package xt.handler.down

import xt._
import xt.handler._

import org.jboss.netty.channel.{SimpleChannelDownstreamHandler, ChannelHandlerContext, MessageEvent, ExceptionEvent}
import org.jboss.netty.handler.codec.http.{HttpRequest, HttpResponse}

trait ResponseHandler extends SimpleChannelDownstreamHandler with Logger {
  override def writeRequested(ctx: ChannelHandlerContext, e: MessageEvent) {
    val m = e.getMessage
    if (!m.isInstanceOf[XtEnv]) {
      ctx.sendDownstream(e)
      return
    }

    val env = m.asInstanceOf[XtEnv]
    handleResponse(ctx, e, env)
  }

  def handleResponse(ctx: ChannelHandlerContext, e: MessageEvent, env: XtEnv)
}
