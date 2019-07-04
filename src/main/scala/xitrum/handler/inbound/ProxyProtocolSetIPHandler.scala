package xitrum.handler.inbound

import scala.util.control.NonFatal

import xitrum.Config
import xitrum.Log
import xitrum.action.Net

import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.haproxy.HAProxyMessage
import io.netty.handler.codec.http.HttpRequest
import io.netty.util.AttributeKey

@Sharable
class ProxyProtocolSetIPHandler extends SimpleChannelInboundHandler[HttpRequest] {

  val HAPROXY_PROTOCOL_MSG = AttributeKey.valueOf("HAProxyMessage").asInstanceOf[AttributeKey[HAProxyMessage]]

  override def channelRead0(ctx: ChannelHandlerContext, request: HttpRequest) {
    val ch = ctx.channel()
    var newRequest:HttpRequest = request
    try {
      Config.xitrum.reverseProxy.foreach(r => {
        r.proxyProtocolEnabledOpt.foreach(proxyProtocolEnabled => {
          if (proxyProtocolEnabled) {
            newRequest = Net.setRemoteIp(ch, request)
          }
        })

      })
      ctx.fireChannelRead(newRequest)
    } catch {
      case NonFatal(e) =>
        Log.debug(s"Could not parse proxy protocol message: ", e)
        BadClientSilencer.respond400(ctx.channel, "Could not parse proxy protocol message")
    }
  }
}
