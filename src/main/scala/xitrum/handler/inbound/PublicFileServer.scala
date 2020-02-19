package xitrum.handler.inbound

import java.io.File

import io.netty.channel.{ChannelHandler, SimpleChannelInboundHandler, ChannelHandlerContext}
import io.netty.handler.codec.http.{HttpMethod, HttpResponseStatus}

import ChannelHandler.Sharable
import HttpMethod._
import HttpResponseStatus._

import xitrum.Config
import xitrum.handler.HandlerEnv
import xitrum.handler.outbound.XSendFile
import xitrum.etag.NotModified
import xitrum.util.PathSanitizer

/**
 * Serves static files in "public" directory.
 * See DefaultHttpChannelInitializer, this handler is put after XSendFile.
 */
@Sharable
class PublicFileServer extends SimpleChannelInboundHandler[HandlerEnv] {
  override def channelRead0(ctx: ChannelHandlerContext, env: HandlerEnv): Unit = {
    val method = env.request.method
    if (method != GET && method != HEAD && method != OPTIONS) {
      ctx.fireChannelRead(env)
      return
    }

    val pathInfo = env.pathInfo.decoded
    if (Config.xitrum.staticFile.pathRegex.findFirstIn(pathInfo).isEmpty) {
      ctx.fireChannelRead(env)
      return
    }

    val response = env.response
    sanitizedAbsStaticPath(pathInfo) match {
      case None =>
        XSendFile.set404Page(response, fromController = false)
        ctx.channel.writeAndFlush(env)

      case Some(abs) =>
        val file = new File(abs)
        if (file.isFile && file.exists) {
          response.setStatus(OK)
          if (method == OPTIONS) {
            ctx.channel.writeAndFlush(env)
          } else {
            if (!Config.xitrum.staticFile.revalidate)
              NotModified.setClientCacheAggressively(response)

            XSendFile.setHeader(response, abs, fromAction = false)
            ctx.channel.writeAndFlush(env)
          }
        } else {
          ctx.fireChannelRead(env)
        }
    }
  }

  /**
   * Sanitizes and returns absolute path.
   *
   * @param pathInfo Starts with "/"
   */
  private def sanitizedAbsStaticPath(pathInfo: String): Option[String] = {
    PathSanitizer.sanitize(pathInfo).map { path =>
      xitrum.root + "/public" + path
    }
  }
}
