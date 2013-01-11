package xitrum.handler.up

import java.io.File

import org.jboss.netty.channel.{ChannelHandler, SimpleChannelUpstreamHandler, ChannelHandlerContext, MessageEvent}
import org.jboss.netty.handler.codec.http.{HttpMethod, HttpResponseStatus, HttpRequest, DefaultHttpResponse, HttpHeaders, HttpVersion}

import ChannelHandler.Sharable
import HttpMethod._
import HttpResponseStatus._
import HttpVersion._

import xitrum.Config
import xitrum.handler.down.XSendFile
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
    if (request.getMethod != GET && request.getMethod != HEAD) {
      ctx.sendUpstream(e)
      return
    }

    val pathInfo = request.getUri.split('?')(0)
    absStaticPath(pathInfo) match {
      case None =>
        val response = new DefaultHttpResponse(HTTP_1_1, OK)
        XSendFile.set404Page(response, false)
        ctx.getChannel.write(response)

      case Some(abs) =>
        val file = new File(abs)
        if (file.isFile && file.exists) {
          val response = new DefaultHttpResponse(HTTP_1_1, OK)

          if (!Config.xitrum.response.clientMustRevalidateStaticFiles)
            NotModified.setClientCacheAggressively(response)

          XSendFile.setHeader(response, abs, false)
          ctx.getChannel.write(response)
        } else {
          ctx.sendUpstream(e)
        }
    }
  }

  //----------------------------------------------------------------------------

  /** Sanitizes and returns absolute path. */
  private def absStaticPath(pathInfo: String): Option[String] = {
    // Convert to absolute path
    // pathInfo starts with "/"
    PathSanitizer.sanitize(pathInfo).map(Config.root + "/public" + _)
  }
}
