package xt.http_handler

import xt._

import org.jboss.netty.channel.{ChannelDownstreamHandler, ChannelHandlerContext, ChannelEvent, MessageEvent}
import org.jboss.netty.handler.codec.http.HttpResponse

trait ResponseHandler extends ChannelDownstreamHandler with Logger {
  def handleDownstream(ctx: ChannelHandlerContext, e: ChannelEvent) {
    if (!e.isInstanceOf[MessageEvent]) {
      ctx.sendDownstream(e)
      return
    }

    val me = e.asInstanceOf[MessageEvent]

    val m = me.getMessage
    if (!m.isInstanceOf[HttpResponse]) {
      ctx.sendDownstream(e)
      return
    }

    val request = m.asInstanceOf[HttpResponse]
    handleResponse(ctx, me, request)
  }

  def handleResponse(ctx: ChannelHandlerContext, e: MessageEvent, response: HttpResponse)
}
