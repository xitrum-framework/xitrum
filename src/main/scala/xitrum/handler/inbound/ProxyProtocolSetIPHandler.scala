package xitrum.handler.inbound

import xitrum.Config

import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.http.HttpRequest

@Sharable
class ProxyProtocolSetIPHandler extends SimpleChannelInboundHandler[HttpRequest] {
  override def channelRead0(ctx: ChannelHandlerContext, request: HttpRequest) {
    if (Config.xitrum.reverseProxy.get.proxyProtocol) ProxyProtocolHandler.setRemoteIp(ctx.channel, request)
    ctx.fireChannelRead(request)
  }
}
