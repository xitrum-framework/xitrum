package xitrum.handler.inbound

import java.io.File

import io.netty.channel.{ChannelHandler, SimpleChannelInboundHandler, ChannelHandlerContext}
import io.netty.handler.codec.http.{HttpMethod, HttpResponseStatus, HttpRequest, HttpHeaders}
import ChannelHandler.Sharable
import HttpMethod._
import HttpResponseStatus._

import xitrum.Config
import xitrum.handler.HandlerEnv
import xitrum.handler.outbound.{XSendFile, XSendResource}
import xitrum.etag.NotModified
import xitrum.util.PathSanitizer

/**
 * Routes /webjars/xxx URL to resources in classpath: http://www.webjars.org/contributing
 * See DefaultHttpChannelInitializer, this handler is put after XSendResource.
 */
@Sharable
class WebJarsServer extends SimpleChannelInboundHandler[HandlerEnv] {
  override def channelRead0(ctx: ChannelHandlerContext, env: HandlerEnv) {
    val request = env.request
    if (request.getMethod != GET && request.getMethod != HEAD && request.getMethod != OPTIONS) {
      ctx.fireChannelRead(env)
      return
    }

    val pathInfo = request.getUri.split('?')(0)
    if (!pathInfo.startsWith("/webjars/")) {
      ctx.fireChannelRead(env)
      return
    }

    val response = env.response
    PathSanitizer.sanitize(pathInfo) match {
      case None =>
        XSendFile.set404Page(response, false)
        ctx.channel.writeAndFlush(env)

      case Some(path) =>
        val resourcePath = "META-INF/resources" + pathInfo

        // Check resource existence, null means the resource does not exist
        val url = getClass.getClassLoader.getResource(resourcePath)

        if (url == null) {
          // Not found, this may be dynamic path (action)
          ctx.fireChannelRead(env)
        } else {
          response.setStatus(OK)
          NotModified.setClientCacheAggressively(response)
          XSendResource.setHeader(response, resourcePath, false)
          ctx.channel.writeAndFlush(env)
        }
    }
  }
}
