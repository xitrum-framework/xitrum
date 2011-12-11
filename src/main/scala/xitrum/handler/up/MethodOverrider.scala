package xitrum.handler.up

import io.netty.channel.{ChannelHandler, SimpleChannelUpstreamHandler, ChannelHandlerContext, MessageEvent, ExceptionEvent, Channels}
import ChannelHandler.Sharable
import io.netty.handler.codec.http.{HttpHeaders, HttpMethod}

import HttpHeaders.Names.UPGRADE
import HttpHeaders.Values.WEBSOCKET
import HttpMethod.POST

import xitrum.handler.HandlerEnv
import xitrum.routing.WebSocketHttpMethod

/**
 * If the real request method is POST and "_method" param exists (taken out by BodyParser),
 * the "_method" param will override the POST method.
 *
 * This middleware should be put behind BodyParser.
 */
@Sharable
class MethodOverrider extends SimpleChannelUpstreamHandler with BadClientSilencer {
  override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {
    val m = e.getMessage
    if (!m.isInstanceOf[HandlerEnv]) {
      ctx.sendUpstream(e)
      return
    }

    val env        = m.asInstanceOf[HandlerEnv]
    val request    = env.request
    val method     = request.getMethod
    val bodyParams = env.bodyParams

    val upgradeHeader = request.getHeader(UPGRADE)
    if (upgradeHeader != null && upgradeHeader.toLowerCase == WEBSOCKET.toLowerCase) {
      request.setMethod(WebSocketHttpMethod)
    } else if (method == POST) {
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
