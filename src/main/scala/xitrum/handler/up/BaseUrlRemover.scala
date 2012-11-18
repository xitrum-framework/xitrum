package xitrum.handler.up

import org.jboss.netty.channel.{ChannelHandler, SimpleChannelUpstreamHandler, ChannelHandlerContext, MessageEvent}
import org.jboss.netty.handler.codec.http.{DefaultHttpResponse, HttpResponseStatus, HttpRequest, HttpVersion}
import ChannelHandler.Sharable

import xitrum.Config
import xitrum.handler.down.XSendFile

@Sharable
class BaseUrlRemover extends SimpleChannelUpstreamHandler with BadClientSilencer {
  override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {
    val m = e.getMessage
    if (!m.isInstanceOf[HttpRequest]) {
      ctx.sendUpstream(e)
      return
    }

    val request = m.asInstanceOf[HttpRequest]
    val channel = ctx.getChannel

    // Attach HttpRequest to the channel so that other handlers can reuse
    channel.setAttachment(request)

    remove(request.getUri) match {
      case None =>
        val response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND)
        XSendFile.set404Page(response, false)
        channel.write(response)

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
