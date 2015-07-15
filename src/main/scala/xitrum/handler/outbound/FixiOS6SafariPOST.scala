package xitrum.handler.outbound

import io.netty.channel.{ChannelHandler, ChannelHandlerContext, ChannelOutboundHandlerAdapter, ChannelPromise}
import io.netty.handler.codec.http.{HttpHeaders, HttpMethod}
import ChannelHandler.Sharable

import xitrum.handler.HandlerEnv
import xitrum.etag.NotModified

/**
 * This handler sets "no-cache" for POST response to fix the problem with
 * iOS 6 Safari:
 * http://www.mnot.net/blog/2012/09/24/caching_POST
 * http://stackoverflow.com/questions/12506897/is-safari-on-ios-6-caching-ajax-results
 */
@Sharable
class FixiOS6SafariPOST extends ChannelOutboundHandlerAdapter {
  override def write(ctx: ChannelHandlerContext, msg: Object, promise: ChannelPromise) {
    if (!msg.isInstanceOf[HandlerEnv]) {
      ctx.write(msg, promise)
      return
    }

    val env      = msg.asInstanceOf[HandlerEnv]
    val request  = env.request
    val response = env.response

    if (request.getMethod == HttpMethod.POST && !response.headers.contains(HttpHeaders.Names.CACHE_CONTROL))
      NotModified.setNoClientCache(response)

    ctx.write(env, promise)
  }
}
