package xitrum.handler.up

import org.jboss.netty.channel.{ChannelHandler, SimpleChannelUpstreamHandler, ChannelHandlerContext, MessageEvent, ExceptionEvent, Channels}
import ChannelHandler.Sharable
import org.jboss.netty.handler.codec.http.{HttpHeaders, HttpMethod}

import HttpHeaders.Names
import HttpHeaders.Values
import HttpMethod.POST

import xitrum.handler.HandlerEnv
import xitrum.routing.HttpMethodWebSocket

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

    // WebSocket should only accept GET
    // "Connection" header must be "Upgrade"
    // "Upgrade" header must be "WebSocket"
    // http://sockjs.github.com/sockjs-protocol/sockjs-protocol-0.3.3.html#section-51
    val connectionHeader = request.getHeader(Names.CONNECTION)
    val upgradeHeader    = request.getHeader(Names.UPGRADE)
    if (connectionHeader != null && connectionHeader.toLowerCase == Values.UPGRADE &&
        upgradeHeader    != null && upgradeHeader.toLowerCase    == Values.WEBSOCKET.toLowerCase &&
        request.getMethod.equals(HttpMethod.GET)) {
      request.setMethod(HttpMethodWebSocket)
    } else if (method == POST) {
      val _methods = bodyParams.get("_method")
      if (_methods != null && _methods.nonEmpty) {
        val _method = new HttpMethod(_methods.get(0).toUpperCase)
        request.setMethod(_method)

        bodyParams.remove("_method")  // Cleaner for application developers when seeing access log
      }
    }

    Channels.fireMessageReceived(ctx, env)
  }
}
