package xitrum.handler.up

import java.io.File

import org.jboss.netty.channel.{ChannelHandler, SimpleChannelUpstreamHandler, ChannelHandlerContext, MessageEvent, ExceptionEvent, Channels}
import org.jboss.netty.handler.codec.http.{HttpMethod, HttpResponseStatus, HttpRequest, DefaultHttpResponse, HttpHeaders, HttpVersion}
import ChannelHandler.Sharable
import HttpMethod._
import HttpResponseStatus._
import HttpVersion._

import xitrum.Config
import xitrum.handler.BaseUri
import xitrum.handler.updown.XSendfile
import xitrum.util.PathSanitizer

/** Serves files in "static/public" directory. */
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

    val pathInfo0 = request.getUri.split('?')(0)

    // See ChannelPipelineFactory, this handler is put after only XSendfile
    // We check baseUri here
    BaseUri.remove(pathInfo0) match {
      case None =>
        val response = new DefaultHttpResponse(HTTP_1_1, NOT_FOUND)
        XSendfile.set404Page(response)
        ctx.getChannel.write(response)
        return

      case Some(pathInfo1) =>
        val withoutSlashPrefix  = pathInfo1.substring(1)
        val isSpecialPublicFile = Config.publicFilesNotBehindPublicUrl.contains(withoutSlashPrefix)

        if (!isSpecialPublicFile && !pathInfo1.startsWith("/public/")) {
          ctx.sendUpstream(e)
          return
        }

        val response = new DefaultHttpResponse(HTTP_1_1, OK)
        absStaticPath(pathInfo1) match {
          case None      => XSendfile.set404Page(response)
          case Some(abs) => XSendfile.setHeader(response, abs)
        }
        ctx.getChannel.write(response)
    }
  }

  //----------------------------------------------------------------------------

  /** Sanitizes and returns absolute path. */
  private def absStaticPath(pathInfo: String): Option[String] = {
    // pathInfo starts with "/"

    PathSanitizer.sanitize(pathInfo) match {
      case None =>
        None

      case Some(path) =>
        // Convert to absolute path
        // user.dir: current working directory
        // See: http://www.java2s.com/Tutorial/Java/0120__Development/Javasystemproperties.htm
        Some(System.getProperty("user.dir") + "/static" + path)
    }
  }
}
