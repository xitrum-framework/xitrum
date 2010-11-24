package xt.handler.up

import xt.Logger

import org.jboss.netty.channel.{SimpleChannelUpstreamHandler, ChannelHandlerContext, MessageEvent, ExceptionEvent, Channels}
import org.jboss.netty.handler.codec.http.{HttpRequest, HttpMethod}
import HttpMethod._

/**
 * If the real request method is POST and "_method" param exists, the "_method"
 * param will override the POST method.
 *
 * This middleware should be put behind BodyParser.
 */
class MethodOverrider extends SimpleChannelUpstreamHandler with Logger {
  override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {
    val m = e.getMessage
    if (!m.isInstanceOf[BodyParserResult]) {
      ctx.sendUpstream(e)
      return
    }

    val bpr     = m.asInstanceOf[BodyParserResult]
    val request = bpr.request
    val method = request.getMethod
    if (method == POST) {
      val _methods = bpr.bodyParams.get("_method")
      if (_methods != null && !_methods.isEmpty) {
        val _method = new HttpMethod(_methods.get(0))
        request.setMethod(_method)
      }
    }

    Channels.fireMessageReceived(ctx, bpr)
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, e: ExceptionEvent) {
    logger.error("MethodOverrider", e.getCause)
    e.getChannel.close
  }
}
