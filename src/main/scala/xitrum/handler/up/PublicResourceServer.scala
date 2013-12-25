package xitrum.handler.up

import java.io.File

import org.jboss.netty.channel.{ChannelHandler, SimpleChannelUpstreamHandler, ChannelHandlerContext, MessageEvent}
import org.jboss.netty.handler.codec.http.{HttpMethod, HttpResponseStatus, HttpRequest, DefaultHttpResponse, HttpHeaders, HttpVersion}
import ChannelHandler.Sharable
import HttpMethod._
import HttpResponseStatus._
import HttpVersion._

import xitrum.Config
import xitrum.handler.HandlerEnv
import xitrum.handler.down.{XSendFile, XSendResource}
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
    if (!m.isInstanceOf[HandlerEnv]) {
      ctx.sendUpstream(e)
      return
    }

    val env     = m.asInstanceOf[HandlerEnv]
    val request = env.request
    if (request.getMethod != GET && request.getMethod != HEAD && request.getMethod != OPTIONS) {
      ctx.sendUpstream(e)
      return
    }

    val pathInfo = request.getUri.split('?')(0)
    if (!pathInfo.startsWith("/resources/public/")) {
      ctx.sendUpstream(e)
      return
    }

    val response = env.response
    response.setStatus(OK)
    PathSanitizer.sanitize(pathInfo) match {
      case None =>
        XSendFile.set404Page(response, false)

      case Some(path) =>
        NotModified.setClientCacheAggressively(response)
        XSendResource.setHeader(response, pathInfo.substring("/resources/".length), false)
    }
    ctx.getChannel.write(env)
  }
}
