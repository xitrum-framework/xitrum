package xitrum.handler.inbound

import io.netty.channel.{ChannelHandler, SimpleChannelInboundHandler, ChannelHandlerContext}
import ChannelHandler.Sharable
import io.netty.handler.codec.http.{HttpHeaderNames, HttpHeaderValues, HttpMethod}
import io.netty.util.AsciiString

import HttpMethod.{GET, POST}

import xitrum.handler.HandlerEnv
import xitrum.routing.HttpMethodWebSocket

/**
 * If the real request method is POST and "_method" param exists, the "_method"
 * param will override the POST method.
 */
@Sharable
class MethodOverrider extends SimpleChannelInboundHandler[HandlerEnv] {
  override def channelRead0(ctx: ChannelHandlerContext, env: HandlerEnv): Unit = {
    val request        = env.request
    val method         = request.method
    val headers        = request.headers
    val bodyTextParams = env.bodyTextParams

    // WebSocket should be GET
    // "Connection" header must contain "Upgrade" (ex: "keep-alive, Upgrade")
    // "Upgrade" header must be "WebSocket"
    // http://sockjs.github.com/sockjs-protocol/sockjs-protocol-0.3.3.html#section-51
    // http://sockjs.github.com/sockjs-protocol/sockjs-protocol-0.3.3.html#section-73
    val connectionHeader = headers.get(HttpHeaderNames.CONNECTION)
    val upgradeHeader    = headers.get(HttpHeaderNames.UPGRADE)
    if (method == GET &&
        connectionHeader != null && AsciiString.containsIgnoreCase(connectionHeader, HttpHeaderValues.UPGRADE) &&
        upgradeHeader    != null && HttpHeaderValues.WEBSOCKET.contentEqualsIgnoreCase(upgradeHeader)) {
      request.setMethod(HttpMethodWebSocket)
    } else if (method == POST) {
      bodyTextParams.get("_method").foreach { _methods =>
        if (_methods.nonEmpty) {
          val _method = new HttpMethod(_methods.head.toUpperCase)
          request.setMethod(_method)
        }

        // Cleaner for application developers when seeing access log
        bodyTextParams.remove("_method")
      }
    }

    ctx.fireChannelRead(env)
  }
}
