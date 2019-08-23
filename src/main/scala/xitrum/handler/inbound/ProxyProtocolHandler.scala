package xitrum.handler.inbound

import io.netty.channel.Channel
import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.haproxy.HAProxyMessage
import io.netty.handler.codec.http.HttpRequest
import io.netty.util.AttributeKey

object ProxyProtocolHandler {
  val HAPROXY_PROTOCOL_SOURCE_IP: AttributeKey[String] =
    AttributeKey.valueOf("HAProxyMessageSourceIp").asInstanceOf[AttributeKey[String]]

  def setRemoteIp(channel: Channel, request: HttpRequest) {
    channel.attr(HAPROXY_PROTOCOL_SOURCE_IP).get() match {
      case sourceIp: String =>
        val headers = request.headers
        val xForwardedFor = headers.get("X-Forwarded-For")
        if (xForwardedFor != null) {
          headers.set("X-Forwarded-For", xForwardedFor.concat(s", ${sourceIp}"))
        } else {
          headers.add("X-Forwarded-For", sourceIp)
        }

      case _ =>
    }
  }
}

@Sharable
class ProxyProtocolHandler extends SimpleChannelInboundHandler[HAProxyMessage] {
  override def channelRead0(ctx: ChannelHandlerContext, msg: HAProxyMessage) {
      ctx.channel.attr(ProxyProtocolHandler.HAPROXY_PROTOCOL_SOURCE_IP).set(msg.sourceAddress)
      ctx.channel.pipeline.remove(this)
  }
}
