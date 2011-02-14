package xitrum.handler.up

import java.io.File

import org.jboss.netty.channel.{ChannelHandler, SimpleChannelUpstreamHandler, ChannelHandlerContext, MessageEvent, ExceptionEvent, Channels}
import org.jboss.netty.handler.codec.http.{HttpRequest, HttpMethod, HttpResponseStatus, HttpVersion, DefaultHttpResponse, HttpHeaders}
import ChannelHandler.Sharable
import HttpMethod._
import HttpResponseStatus._
import HttpVersion._

import xitrum.handler.Env
import xitrum.vc.env.PathInfo

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
    if (!m.isInstanceOf[Env]) {
      ctx.sendUpstream(e)
      return
    }

    val env      = m.asInstanceOf[Env]
    val request  = env("request").asInstanceOf[HttpRequest]
    val pathInfo = env("pathInfo").asInstanceOf[PathInfo]
    val decoded  = pathInfo.decoded

    if (request.getMethod != GET) {
      Channels.fireMessageReceived(ctx, env)
      return
    }

    val decoded2 = if (decoded == "/favicon.ico" || decoded == "/robots.txt")
      "/public" + decoded
    else
      decoded

    if (!decoded2.startsWith("/public/")) {
      Channels.fireMessageReceived(ctx, env)
      return
    }

    val response = new DefaultHttpResponse(HTTP_1_1, OK)
    sanitizePathInfo(decoded2) match {
      case Some(abs) =>
        response.setHeader("X-Sendfile", abs)

      case None =>
        response.setStatus(NOT_FOUND)
        HttpHeaders.setContentLength(response, 0)

    }
    env("response") = response
    ctx.getChannel.write(env)
  }

  //----------------------------------------------------------------------------

  /**
   * @return None if decodedPathInfo is invalid or the corresponding file is hidden,
   *         otherwise Some(the absolute file path)
   */
  private def sanitizePathInfo(decodedPathInfo: String): Option[String] = {
    // pathInfo starts with "/"

    // Convert file separators
    val path = decodedPathInfo.replace('/', File.separatorChar)

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
