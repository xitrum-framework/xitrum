package xitrum.handler.inbound

import xitrum.Config

import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.channel.ChannelHandler.Sharable
import io.netty.handler.codec.haproxy.HAProxyMessage
import io.netty.handler.codec.http.HttpRequest

object ProxyProtocolHandler {
  var remoteIp:String = ""

  def setRemoteIpFromProxyMessage(sourceIp: String): Unit = {
    remoteIp = sourceIp
  }
  def setRemoteIp(request: HttpRequest) {
    if (!remoteIp.isEmpty) {
      val headers = request.headers
      val xForwardedFor = headers.get("X-Forwarded-For")
      if (xForwardedFor != null) {
        headers.set("X-Forwarded-For", xForwardedFor.concat(s", ${remoteIp}"))
      } else {
        headers.add("X-Forwarded-For", remoteIp)
      }
    }
  }
}

@Sharable
class ProxyProtocolHandler extends ChannelInboundHandlerAdapter {
  override def channelRead(ctx: ChannelHandlerContext, msg: Object) {
    if (Config.xitrum.reverseProxy.get.proxyProtocol && msg.isInstanceOf[HAProxyMessage]) {
      val haProxyMessage = msg.asInstanceOf[HAProxyMessage]
      ProxyProtocolHandler.setRemoteIpFromProxyMessage(haProxyMessage.sourceAddress)
      // Fix memory leak
      // https://github.com/netty/netty/pull/9250
      haProxyMessage.release()
      ctx.channel.pipeline.remove(classOf[ProxyProtocolHandler])
    } else {
      ctx.fireChannelRead(msg)
    }
  }
}
