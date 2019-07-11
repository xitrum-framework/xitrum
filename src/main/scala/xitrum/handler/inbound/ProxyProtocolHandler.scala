package xitrum.handler.inbound

import scala.util.control.NonFatal

import xitrum.Config
import xitrum.Log

import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.handler.codec.haproxy.HAProxyMessage
import io.netty.util.AttributeKey

@Sharable
class ProxyProtocolHandler extends ChannelInboundHandlerAdapter {

  val HAPROXY_PROTOCOL_MSG = AttributeKey.valueOf("HAProxyMessage").asInstanceOf[AttributeKey[HAProxyMessage]]

  override def channelRead(ctx: ChannelHandlerContext, msg: Object) {
    val ch = ctx.channel()
    try {
      if (Config.xitrum.proxyProtocolEnabled) {
        msg match {
          case haMsg:HAProxyMessage =>
            ch.attr(HAPROXY_PROTOCOL_MSG).set(haMsg)
          case _ =>
            ctx.fireChannelRead(msg)
        }
      } else {
        ctx.fireChannelRead(msg)
      }
    } catch {
      case NonFatal(e) =>
        Log.debug(s"Could not parse proxy protocol message: ", e)
        BadClientSilencer.respond400(ctx.channel, "Could not parse proxy protocol message")
    }
  }
}
