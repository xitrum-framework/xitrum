package xitrum.handler.up

import io.netty.channel.{ChannelHandler, SimpleChannelUpstreamHandler, ChannelHandlerContext, MessageEvent}
import ChannelHandler.Sharable
import io.netty.handler.codec.http.{DefaultHttpResponse, HttpResponseStatus, HttpRequest, HttpVersion}

import xitrum.Config
import xitrum.handler.updown.XSendFile

@Sharable
class BaseUriRemover extends SimpleChannelUpstreamHandler with BadClientSilencer {
  override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {
    val m = e.getMessage
    if (!m.isInstanceOf[HttpRequest]) {
      ctx.sendUpstream(e)
      return
    }

    val request = m.asInstanceOf[HttpRequest]
    remove(request.getUri) match {
      case None =>
        val response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND)
        XSendFile.set404Page(response)
        ctx.getChannel.write(response)

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
    if (originalUri == Config.baseUri)
      Some("/")
    else if (originalUri.startsWith(Config.baseUri + "/"))
      Some(originalUri.substring(Config.baseUri.length))
    else
      None
  }
}
