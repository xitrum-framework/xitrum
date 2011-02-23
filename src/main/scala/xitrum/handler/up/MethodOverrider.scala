package xitrum.handler.up

import org.jboss.netty.channel.{ChannelHandler, SimpleChannelUpstreamHandler, ChannelHandlerContext, MessageEvent, ExceptionEvent, Channels}
import ChannelHandler.Sharable
import org.jboss.netty.handler.codec.http.HttpMethod
import HttpMethod._

import xitrum.handler.Env

/**
 * If the real request method is POST and "_method" param exists, the "_method"
 * param will override the POST method.
 *
 * This middleware should be put behind BodyParser.
 */
@Sharable
class MethodOverrider extends SimpleChannelUpstreamHandler with ClosedClientSilencer {
  override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {
    val m = e.getMessage
    if (!m.isInstanceOf[Env]) {
      ctx.sendUpstream(e)
      return
    }

    val env        = m.asInstanceOf[Env]
    val request    = env.request
    val method     = request.getMethod
    val bodyParams = env.bodyParams
    if (method == POST) {
      val _methods = bodyParams.get("_method")
      if (_methods != null && !_methods.isEmpty) {
        val _method = new HttpMethod(_methods.get(0).toUpperCase)
        request.setMethod(_method)

        bodyParams.remove("_method")  // Cleaner for application developers when seeing access log
      }
    }

    Channels.fireMessageReceived(ctx, env)
  }
}
