package xt.http_handler

import xt._

import org.jboss.netty.channel.{ChannelHandlerContext, MessageEvent, Channels, ChannelFutureListener}
import org.jboss.netty.handler.codec.http.{HttpRequest, HttpResponse, HttpHeaders}

class KeepaliveHandler extends ResponseHandler {
  def handleResponse(ctx: ChannelHandlerContext, e: MessageEvent, response: HttpResponse) {
    val request = ctx.getAttachment.asInstanceOf[HttpRequest]
    if (HttpHeaders.isKeepAlive(request)) {
      ctx.sendDownstream(e)
    } else {
      val future = e.getFuture
      future.addListener(ChannelFutureListener.CLOSE)
      Channels.write(ctx, future, response)
    }
  }
}
