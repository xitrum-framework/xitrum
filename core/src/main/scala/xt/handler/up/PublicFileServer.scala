package xt.handler.up

import xt._
import xt.vc.Env

import java.io.File

import org.jboss.netty.channel._
import org.jboss.netty.handler.codec.http._
import HttpMethod._
import HttpResponseStatus._
import HttpVersion._

/**
 * Serves special files or files in /public directory.
 *
 * Special files:
 *    favicon.ico may be not at the root: http://en.wikipedia.org/wiki/Favicon
 *    robots.txt     must be at the root: http://en.wikipedia.org/wiki/Robots_exclusion_standard
 */
class PublicFileServer extends RequestHandler {
  def handleRequest(ctx: ChannelHandlerContext, env: Env) {
    import env._

    if (method != GET) {
      Channels.fireMessageReceived(ctx, env)
      return
    }

    val pathInfo2 = if (pathInfo == "/favicon.ico" || pathInfo == "/robots.txt")
      "/public" + pathInfo
    else
      pathInfo

    if (!pathInfo2.startsWith("/public/")) {
      Channels.fireMessageReceived(ctx, env)
      return
    }

    sanitizePathInfo(pathInfo2) match {
      case Some(abs) =>
        response.setHeader("X-Sendfile", abs)

      case None =>
        response.setStatus(NOT_FOUND)
        HttpHeaders.setContentLength(response, 0)
    }
    respond(ctx, env)
  }

  /**
   * @return None if pathInfo is invalid or the corresponding file is hidden,
   *         otherwise Some(the absolute file path)
   */
  private def sanitizePathInfo(pathInfo: String): Option[String] = {
    // pathInfo starts with "/"

    var decoded: String = null

    URLDecoder.decode(pathInfo) match {
      case None => None

      case Some(decoded) =>
        // Convert file separators
        val decoded2 = decoded.replace('/', File.separatorChar)

        // Simplistic dumb security check
        if (decoded2.contains(File.separator + ".") ||
            decoded2.contains("." + File.separator) ||
            decoded2.startsWith(".")                ||
            decoded2.endsWith(".")) {
          None
        } else {
          // Convert to absolute path
          // user.dir: current working directory
          // See: http://www.java2s.com/Tutorial/Java/0120__Development/Javasystemproperties.htm
          val abs = System.getProperty("user.dir") + decoded2

          // For security do not return hidden file
          val file = new File(abs)
          if (!file.exists() || file.isHidden()) None else Some(abs)
        }
    }
  }
}
