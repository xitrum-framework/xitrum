package xitrum.handler.inbound

import io.netty.channel.{ChannelHandler, SimpleChannelInboundHandler, ChannelHandlerContext}
import ChannelHandler.Sharable

import xitrum.Config
import xitrum.handler.HandlerEnv
import xitrum.handler.outbound.XSendFile

@Sharable
class BaseUrlRemover extends SimpleChannelInboundHandler[HandlerEnv] {
  override def channelRead0(ctx: ChannelHandlerContext, env: HandlerEnv) {
    val request = env.request

    remove(request.getUri) match {
      case None =>
        val response = env.response
        XSendFile.set404Page(response, false)
        ctx.channel.writeAndFlush(env)

      case Some(withoutBaseUri) =>
        request.setUri(withoutBaseUri)
        ctx.fireChannelRead(env)
    }
  }

  /**
   * Removes the base URI (see config/xitrum.properties) from the original request URL.
   *
   * @return None if the original URL does not start with the base URI
   */
  private def remove(originalUri: String): Option[String] = {
    if (originalUri == Config.baseUrl)
      Some("/")
    else if (originalUri.startsWith(Config.baseUrl + "/"))
      Some(originalUri.substring(Config.baseUrl.length))
    else
      None
  }
}
