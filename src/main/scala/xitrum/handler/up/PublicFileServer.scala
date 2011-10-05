package xitrum.handler.up

import java.io.File

import org.jboss.netty.channel.{ChannelHandler, SimpleChannelUpstreamHandler, ChannelHandlerContext, MessageEvent}
import org.jboss.netty.handler.codec.http.{HttpMethod, HttpResponseStatus, HttpRequest, DefaultHttpResponse, HttpHeaders, HttpVersion}
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

    val pathInfo            = request.getUri.split('?')(0)
    val withoutSlashPrefix  = pathInfo.substring(1)
    val isSpecialPublicFile = Config.publicFilesNotBehindPublicUrl.contains(withoutSlashPrefix)

    if (!isSpecialPublicFile && !pathInfo.startsWith("/public/")) {
      ctx.sendUpstream(e)
      return
    }

    val response = new DefaultHttpResponse(HTTP_1_1, OK)
    absStaticPath(pathInfo) match {
      case None => XSendFile.set404Page(response)

      case Some(abs) =>
        NotModified.setMaxAgeAggressively(response)
        XSendFile.setHeader(response, abs)
    }
    ctx.getChannel.write(response)
  }

  //----------------------------------------------------------------------------

  /** Sanitizes and returns absolute path. */
  private def absStaticPath(pathInfo: String): Option[String] = {
    // pathInfo starts with "/"
    PathSanitizer.sanitize(pathInfo) match {
      case None => None

      case Some(path) =>
        // Convert to absolute path
        // user.dir: current working directory
        // See: http://www.java2s.com/Tutorial/Java/0120__Development/Javasystemproperties.htm
        Some(System.getProperty("user.dir") + "/static" + path)
    }
  }
}
