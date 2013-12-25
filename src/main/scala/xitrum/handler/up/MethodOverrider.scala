package xitrum.handler.up

import org.jboss.netty.channel.{ChannelHandler, SimpleChannelUpstreamHandler, ChannelHandlerContext, MessageEvent, ExceptionEvent, Channels}
import ChannelHandler.Sharable
import org.jboss.netty.handler.codec.http.{HttpHeaders, HttpMethod}

import HttpHeaders.Names
import HttpHeaders.Values
import HttpMethod.{GET, POST}

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

    val env            = m.asInstanceOf[HandlerEnv]
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

    ctx.sendUpstream(e)
  }
}
