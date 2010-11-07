package xt.http_handler

import xt._

import org.jboss.netty.channel.{ChannelUpstreamHandler, ChannelHandlerContext, ChannelEvent, MessageEvent}
import org.jboss.netty.handler.codec.http.{HttpRequest, HttpResponse}

trait RequestHandler extends ChannelUpstreamHandler with Logger {
  def handleUpstream(ctx: ChannelHandlerContext, e: ChannelEvent) {
    if (!e.isInstanceOf[MessageEvent]) {
      ctx.sendUpstream(e)
      return
    }

    val me = e.asInstanceOf[MessageEvent]

    val m = me.getMessage
    if (!m.isInstanceOf[HttpRequest]) {
      ctx.sendUpstream(e)
      return
    }

    val request = m.asInstanceOf[HttpRequest]
    handleRequest(ctx, me, request)
  }

  def handleRequest(ctx: ChannelHandlerContext, e: MessageEvent, request: HttpRequest)

  protected def respond(ctx: ChannelHandlerContext, request: HttpRequest, response: HttpResponse) {
    ctx.setAttachment(request)
    ctx.getChannel.write(response)
  }
}
