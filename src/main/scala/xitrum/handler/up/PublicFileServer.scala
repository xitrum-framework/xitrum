package xitrum.handler.up

import java.io.File

import io.netty.channel.{ChannelHandler, SimpleChannelUpstreamHandler, ChannelHandlerContext, MessageEvent}
import io.netty.handler.codec.http.{HttpMethod, HttpResponseStatus, HttpRequest, DefaultHttpResponse, HttpHeaders, HttpVersion}
import ChannelHandler.Sharable
import HttpMethod._
import HttpResponseStatus._
import HttpVersion._

import xitrum.Config
import xitrum.handler.updown.XSendFile
import xitrum.etag.NotModified
import xitrum.util.PathSanitizer

/**
 * Serves files in "static/public" directory.
 * See ChannelPipelineFactory, this handler is put after XSendFile.
 */
@Sharable
class PublicFileServer extends SimpleChannelUpstreamHandler with BadClientSilencer {
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
    absStaticPath(pathInfo) match {
      case None =>
        val response = new DefaultHttpResponse(HTTP_1_1, OK)
        XSendFile.set404Page(response)
        ctx.getChannel.write(response)

      case Some(abs) =>
        val file = new File(abs)
        if (file.isFile && file.exists) {
          val response = new DefaultHttpResponse(HTTP_1_1, OK)
          NotModified.setClientCacheAggressively(response)
          XSendFile.setHeader(response, abs)
          ctx.getChannel.write(response)
        } else {
          ctx.sendUpstream(e)
        }
    }
  }

  //----------------------------------------------------------------------------

  /** Sanitizes and returns absolute path. */
  private def absStaticPath(pathInfo: String): Option[String] = {
    PathSanitizer.sanitize(pathInfo) match {
      case None => None

      case Some(path) =>
        // Convert to absolute path
        // pathInfo starts with "/"
        Some(Config.root + "/public" + path)
    }
  }
}
