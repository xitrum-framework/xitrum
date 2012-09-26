package xitrum.handler.down

import org.jboss.netty.channel.{ChannelEvent, ChannelDownstreamHandler, ChannelHandler, ChannelHandlerContext, DownstreamMessageEvent}
import org.jboss.netty.handler.codec.http.{HttpHeaders, HttpMethod, HttpRequest, HttpResponse}
import ChannelHandler.Sharable

import xitrum.etag.NotModified

/**
 * This handler sets "no-cache" for POST response to fix the problem with
 * iOS 6 Safari:
 * http://www.mnot.net/blog/2012/09/24/caching_POST
 * http://stackoverflow.com/questions/12506897/is-safari-on-ios-6-caching-ajax-results
 */
@Sharable
class FixiOS6SafariPOST extends ChannelDownstreamHandler {
  def handleDownstream(ctx: ChannelHandlerContext, e: ChannelEvent) {
    if (!e.isInstanceOf[DownstreamMessageEvent]) {
      ctx.sendDownstream(e)
      return
    }

    val m = e.asInstanceOf[DownstreamMessageEvent].getMessage
    if (!m.isInstanceOf[HttpResponse]) {
      ctx.sendDownstream(e)
      return
    }

    val response = m.asInstanceOf[HttpResponse]
    val request  = ctx.getChannel.getAttachment.asInstanceOf[HttpRequest]
    if (request.getMethod == HttpMethod.POST && !response.containsHeader(HttpHeaders.Names.CACHE_CONTROL))
      NotModified.setNoCacheHeader(response)

    ctx.sendDownstream(e)
  }
}
