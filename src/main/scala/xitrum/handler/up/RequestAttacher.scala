package xitrum.handler.up

import org.jboss.netty.channel.{ChannelEvent, ChannelHandlerContext}
import org.jboss.netty.channel.ChannelHandler.Sharable
import org.jboss.netty.handler.codec.http.HttpRequest
import org.jboss.netty.channel.SimpleChannelUpstreamHandler
import org.jboss.netty.channel.MessageEvent

object RequestAttacher {
  /**
   * FixiOS6SafariPOST, XSendFile, and XSendResource require the HttpRequest to be
   * attached to the current channel. In those handlers, return as soon as
   * possible if the HttpRequest is not there.
   *
   * See DefaultHttpChannelPipelineFactory.
   */
  def retrieveOrSendDownstream(ctx: ChannelHandlerContext, e: ChannelEvent): HttpRequest = {
    val request = ctx.getChannel.getAttachment.asInstanceOf[HttpRequest]
    if (request == null) ctx.sendDownstream(e)
    request
  }
}

/**
 * FixiOS6SafariPOST, XSendFile, and XSendResource require the HttpRequest to be
 * attached to the current channel.
 */
@Sharable
class RequestAttacher extends SimpleChannelUpstreamHandler with BadClientSilencer {
  override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {
    val m = e.getMessage
    if (!m.isInstanceOf[HttpRequest]) {
      ctx.sendUpstream(e)
      return
    }

    val request = m.asInstanceOf[HttpRequest]
    ctx.getChannel.setAttachment(request)
    ctx.sendUpstream(e)
  }
}
