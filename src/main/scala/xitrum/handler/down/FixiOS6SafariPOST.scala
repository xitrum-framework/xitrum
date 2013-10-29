package xitrum.handler.down

import org.jboss.netty.channel.{ChannelEvent, ChannelDownstreamHandler, ChannelHandler, ChannelHandlerContext, DownstreamMessageEvent}
import org.jboss.netty.handler.codec.http.{HttpHeaders, HttpMethod, HttpRequest, HttpResponse}
import ChannelHandler.Sharable

import xitrum.Log
import xitrum.etag.NotModified
import xitrum.handler.up.RequestAttacher

/**
 * This handler sets "no-cache" for POST response to fix the problem with
 * iOS 6 Safari:
 * http://www.mnot.net/blog/2012/09/24/caching_POST
 * http://stackoverflow.com/questions/12506897/is-safari-on-ios-6-caching-ajax-results
 */
@Sharable
class FixiOS6SafariPOST extends ChannelDownstreamHandler with Log {
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

    val request = RequestAttacher.retrieveOrSendDownstream(ctx, e)
    if (request == null) return

    val response = m.asInstanceOf[HttpResponse]

    // This is the last Xitrum handler, log the response
    if (log.isTraceEnabled) log.trace(response.toString)

    if (request.getMethod == HttpMethod.POST && !response.containsHeader(HttpHeaders.Names.CACHE_CONTROL))
      NotModified.setNoClientCache(response)

    ctx.sendDownstream(e)
  }
}
