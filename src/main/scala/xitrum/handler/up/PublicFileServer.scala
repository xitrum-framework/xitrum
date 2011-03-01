package xitrum.handler.up

import java.io.File

import org.jboss.netty.channel.{ChannelHandler, SimpleChannelUpstreamHandler, ChannelHandlerContext, MessageEvent, ExceptionEvent, Channels}
import org.jboss.netty.handler.codec.http.{HttpMethod, HttpResponseStatus, HttpRequest, DefaultHttpResponse, HttpHeaders, HttpVersion}
import ChannelHandler.Sharable
import HttpMethod._
import HttpResponseStatus._
import HttpVersion._

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
    sanitizePathInfo(pathInfo2) match {
      case Some(abs) =>
        response.setHeader(XSendfile.XSENDFILE_HEADER, abs)

      case None =>
        response.setStatus(NOT_FOUND)
        HttpHeaders.setContentLength(response, 0)
    }
    ctx.getChannel.write(response)
  }

  //----------------------------------------------------------------------------

  /**
   * @return None if pathInfo is invalid or the corresponding file is hidden,
   *         otherwise Some(the absolute file path)
   */
  private def sanitizePathInfo(pathInfo: String): Option[String] = {
    // pathInfo starts with "/"

    // Convert file separators
    val path = pathInfo.replace('/', File.separatorChar)

    // Simplistic dumb security check
    if (path.contains(File.separator + ".") ||
        path.contains("." + File.separator) ||
        path.startsWith(".")                ||
        path.endsWith(".")) {
      None
    } else {
      // Convert to absolute path
      // user.dir: current working directory
      // See: http://www.java2s.com/Tutorial/Java/0120__Development/Javasystemproperties.htm
      val abs = System.getProperty("user.dir") + path
      Some(abs)
    }
  }
}
