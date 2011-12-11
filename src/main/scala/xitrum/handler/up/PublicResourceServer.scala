package xitrum.handler.up

import java.io.File

import io.netty.channel.{ChannelHandler, SimpleChannelUpstreamHandler, ChannelHandlerContext, MessageEvent}
import io.netty.handler.codec.http.{HttpMethod, HttpResponseStatus, HttpRequest, DefaultHttpResponse, HttpHeaders, HttpVersion}
import ChannelHandler.Sharable
import HttpMethod._
import HttpResponseStatus._
import HttpVersion._

import xitrum.Config
import xitrum.handler.updown.{XSendFile, XSendResource}
import xitrum.etag.NotModified
import xitrum.util.PathSanitizer

/**
 * Routes /resources/public/xxx URL to resources in classpath.
 * See ChannelPipelineFactory, this handler is put after XSendResource.
 */
@Sharable
class PublicResourceServer extends SimpleChannelUpstreamHandler with BadClientSilencer {
  override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {
    val m = e.getMessage
    if (!m.isInstanceOf[HttpRequest]) {
      ctx.sendUpstream(e)
      return
    }

    val request = m.asInstanceOf[HttpRequest]
    if (request.getMethod != GET) {
      ctx.sendUpstream(e)
      return
    }

    val pathInfo = request.getUri.split('?')(0)
    if (!pathInfo.startsWith("/resources/public/")) {
      ctx.sendUpstream(e)
      return
    }

    val response = new DefaultHttpResponse(HTTP_1_1, OK)
    PathSanitizer.sanitize(pathInfo) match {
      case None => XSendFile.set404Page(response)

      case Some(path) =>
        NotModified.setClientCacheAggressively(response)
        XSendResource.setHeader(response, pathInfo.substring("/resources/".length))
    }
    ctx.getChannel.write(response)
  }
}
