package xitrum.handler.inbound

import io.netty.channel.Channel
import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.haproxy.HAProxyMessage
import io.netty.handler.codec.http.HttpRequest
import io.netty.util.AttributeKey

object ProxyProtocolHandler {
  val HAPROXY_PROTOCOL_MSG: AttributeKey[HAProxyMessage] =
    AttributeKey.valueOf("HAProxyMessage").asInstanceOf[AttributeKey[HAProxyMessage]]

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
class ProxyProtocolHandler extends SimpleChannelInboundHandler[HAProxyMessage] {
  override def channelRead0(ctx: ChannelHandlerContext, msg: HAProxyMessage) {
      ctx.channel.attr(ProxyProtocolHandler.HAPROXY_PROTOCOL_MSG).set(msg)
      ctx.channel.pipeline.remove(classOf[ProxyProtocolHandler])
  }
}
