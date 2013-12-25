package xitrum.handler.up

import org.jboss.netty.channel.{ChannelHandler, SimpleChannelUpstreamHandler, ChannelHandlerContext, MessageEvent}
import org.jboss.netty.handler.codec.http.{DefaultHttpResponse, HttpResponseStatus, HttpRequest, HttpVersion}
import ChannelHandler.Sharable

import xitrum.Config
import xitrum.handler.HandlerEnv
import xitrum.handler.down.XSendFile

@Sharable
class BaseUrlRemover extends SimpleChannelUpstreamHandler with BadClientSilencer {
  override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {
    val m = e.getMessage
    if (!m.isInstanceOf[HandlerEnv]) {
      ctx.sendUpstream(e)
      return
    }

    val env     = m.asInstanceOf[HandlerEnv]
    val request = env.request
    val channel = ctx.getChannel

    remove(request.getUri) match {
      case None =>
        val response = env.response
        response.setStatus(HttpResponseStatus.NOT_FOUND)
        XSendFile.set404Page(response, false)
        channel.write(env)

      case Some(withoutBaseUri) =>
        request.setUri(withoutBaseUri)
        ctx.sendUpstream(e)
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
