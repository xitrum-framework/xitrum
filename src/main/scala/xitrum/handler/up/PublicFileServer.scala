package xitrum.handler.up

import java.io.File

import org.jboss.netty.channel.{ChannelHandler, SimpleChannelUpstreamHandler, ChannelHandlerContext, MessageEvent, ExceptionEvent, Channels}
import org.jboss.netty.handler.codec.http.{HttpMethod, HttpResponseStatus, HttpRequest, DefaultHttpResponse, HttpHeaders, HttpVersion}
import ChannelHandler.Sharable
import HttpMethod._
import HttpResponseStatus._
import HttpVersion._

import xitrum.PathSanitizer
import xitrum.handler.updown.XSendfile

/**
 * Serves special files or files in /public directory.
 *
 * Special files:
 *    favicon.ico may be not at the root: http://en.wikipedia.org/wiki/Favicon
 *    robots.txt     must be at the root: http://en.wikipedia.org/wiki/Robots_exclusion_standard
 */
@Sharable
class PublicFileServer extends SimpleChannelUpstreamHandler with ClosedClientSilencer {
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

    val pathInfo  = request.getUri.split('?')(0)
    val pathInfo2 = if (pathInfo.startsWith("/favicon.ico") || pathInfo.startsWith("/robots.txt")) "/public" + pathInfo else pathInfo

    if (!pathInfo2.startsWith("/public/")) {
      ctx.sendUpstream(e)
      return
    }

    val response = new DefaultHttpResponse(HTTP_1_1, OK)
    toAbsPath(pathInfo2) match {
      case None      => XSendfile.set404Page(response)
      case Some(abs) => XSendfile.setHeader(response, abs)
    }
    ctx.getChannel.write(response)
  }

  //----------------------------------------------------------------------------

  /** Sanitizes and returns absolute path. */
  private def toAbsPath(pathInfo: String): Option[String] = {
    // pathInfo starts with "/"

    PathSanitizer.sanitize(pathInfo) match {
      case None =>
        None

      case Some(path) =>
        // Convert to absolute path
        // user.dir: current working directory
        // See: http://www.java2s.com/Tutorial/Java/0120__Development/Javasystemproperties.htm
        Some(System.getProperty("user.dir") + path)
    }
  }
}
