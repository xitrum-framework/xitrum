package xt.handler.up

import xt.handler.Env
import xt.vc.env.{Env => CEnv}

import org.jboss.netty.channel.{ChannelHandler, SimpleChannelUpstreamHandler, ChannelHandlerContext, MessageEvent, ExceptionEvent, Channels}
import ChannelHandler.Sharable
import org.jboss.netty.handler.codec.http.{HttpRequest, HttpMethod}
import HttpMethod._

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
    val request    = env("request").asInstanceOf[HttpRequest]
    val method     = request.getMethod
    val bodyParams = env("bodyParams").asInstanceOf[CEnv.Params]
    if (method == POST) {
      val _methods = bodyParams.get("_method")
      if (_methods != null && !_methods.isEmpty) {
        val _method = new HttpMethod(_methods.get(0))
        request.setMethod(_method)
      }
    }

    Channels.fireMessageReceived(ctx, env)
  }
}
