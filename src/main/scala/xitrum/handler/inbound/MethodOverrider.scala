package xitrum.handler.inbound

import io.netty.channel.{ChannelHandler, SimpleChannelInboundHandler, ChannelHandlerContext}
import ChannelHandler.Sharable
import io.netty.handler.codec.http.{HttpHeaders, HttpMethod}

import HttpHeaders.Names
import HttpHeaders.Values
import HttpMethod.{GET, POST}

import xitrum.handler.HandlerEnv
import xitrum.routing.HttpMethodWebSocket

/**
 * If the real request method is POST and "_method" param exists, the "_method"
 * param will override the POST method.
 */
@Sharable
class MethodOverrider extends SimpleChannelInboundHandler[HandlerEnv] {
  override def channelRead0(ctx: ChannelHandlerContext, env: HandlerEnv) {
    val request        = env.request
    val method         = request.getMethod
    val bodyTextParams = env.bodyTextParams

    // WebSocket should be GET
    // "Connection" header must contain "Upgrade" (ex: "keep-alive, Upgrade")
    // "Upgrade" header must be "WebSocket"
    // http://sockjs.github.com/sockjs-protocol/sockjs-protocol-0.3.3.html#section-51
    // http://sockjs.github.com/sockjs-protocol/sockjs-protocol-0.3.3.html#section-73
    val connectionHeader = HttpHeaders.getHeader(request, Names.CONNECTION)
    val upgradeHeader    = HttpHeaders.getHeader(request, Names.UPGRADE)
    if (method == GET &&
        connectionHeader != null && connectionHeader.toLowerCase.contains(Values.UPGRADE.toLowerCase) &&
        upgradeHeader    != null && upgradeHeader.toLowerCase == Values.WEBSOCKET.toLowerCase) {
      request.setMethod(HttpMethodWebSocket)
    } else if (method == POST) {
      val _methods = bodyTextParams.get("_method")
      if (_methods != null && _methods.nonEmpty) {
        val _method = new HttpMethod(_methods.get(0).toUpperCase)
        request.setMethod(_method)
        bodyTextParams.remove("_method")  // Cleaner for application developers when seeing access log
      }
    }

    ctx.fireChannelRead(env)
  }
}
