package xitrum.handler.inbound

import xitrum.Config
import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel.{Channel, ChannelHandlerContext, ChannelInboundHandlerAdapter}
import io.netty.handler.codec.haproxy.HAProxyMessage
import io.netty.handler.codec.http.HttpRequest
import io.netty.util.AttributeKey

object ProxyProtocolHandler {
  val HAPROXY_PROTOCOL_MSG: AttributeKey[HAProxyMessage] =
    AttributeKey.valueOf("HAProxyMessage").asInstanceOf[AttributeKey[HAProxyMessage]]

  def setHAProxyMessage(channel: Channel, msg: HAProxyMessage) {
    channel.attr(HAPROXY_PROTOCOL_MSG).set(msg)
    // Fix memory leak
    // https://github.com/netty/netty/pull/9250
    msg.release()
  }

  def setRemoteIp(channel: Channel, request: HttpRequest) {
    channel.attr(HAPROXY_PROTOCOL_MSG).get() match {
      case haMsg: HAProxyMessage =>
        val headers = request.headers
        val xForwardedFor = headers.get("X-Forwarded-For")
        if (xForwardedFor != null) {
          headers.set("X-Forwarded-For", xForwardedFor.concat(s", ${haMsg.sourceAddress}"))
        } else {
          headers.add("X-Forwarded-For", haMsg.sourceAddress)
        }

      case _ =>
    }
  }
}

@Sharable
class ProxyProtocolHandler extends ChannelInboundHandlerAdapter {
  override def channelRead(ctx: ChannelHandlerContext, msg: Object) {
    if (Config.xitrum.reverseProxy.get.proxyProtocol && msg.isInstanceOf[HAProxyMessage]) {
      ProxyProtocolHandler.setHAProxyMessage(ctx.channel, msg.asInstanceOf[HAProxyMessage])
      ctx.channel.pipeline.remove(classOf[ProxyProtocolHandler])
    } else {
      ctx.fireChannelRead(msg)
    }
  }
}
