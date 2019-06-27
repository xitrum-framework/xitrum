package xitrum.handler.inbound

import scala.util.control.NonFatal

import xitrum.Config
import xitrum.Log
import xitrum.handler.HandlerEnv

import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.haproxy.HAProxyMessage
import io.netty.util.AttributeKey

@Sharable
class ProxyProtocolHandler extends SimpleChannelInboundHandler[HandlerEnv] {

  val HAPROXY_PROTOCOL_MSG = AttributeKey.valueOf("HAProxyMessage").asInstanceOf[AttributeKey[HAProxyMessage]]

  override def channelRead0(ctx: ChannelHandlerContext, env: HandlerEnv) {
    val ch = ctx.channel()
    val request = env.request
    try {
      Config.xitrum.reverseProxy.foreach(r => {
        r.proxyProtocolEnabledOpt.foreach(proxyProtocolEnabled => {
          if (proxyProtocolEnabled) {
            val params = ch.attr(HAPROXY_PROTOCOL_MSG).get()
            request.headers().
              add("X-Source-Address", params.sourceAddress()).
              add("X-Destination-Address", params.destinationAddress()).
              add("X-Source-Port", params.sourcePort()).
              add("X-Destination-Port", params.destinationPort())

          }
        })

      })
      ctx.fireChannelRead(env)
    } catch {
      case NonFatal(e) =>
        Log.debug(s"Could not parse proxy protocol message: ", e)
        BadClientSilencer.respond400(ctx.channel, "Could not parse proxy protocol message")
    }
  }
}
