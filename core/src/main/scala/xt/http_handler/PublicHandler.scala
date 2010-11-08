package xt.http_handler

import xt._

import java.io.File

import org.jboss.netty.channel._
import org.jboss.netty.handler.codec.http._
import HttpResponseStatus._
import HttpVersion._

class PublicHandler extends RequestHandler {
  def handleRequest(ctx: ChannelHandlerContext, env: XtEnv) {
    import env._

    val uri = request.getUri
    if (!uri.startsWith("/public")) {
      Channels.fireMessageReceived(ctx, env)
      return
    }

    sanitizeUri(uri) match {
      case Some(abs) =>
        response.setHeader("X-Sendfile", abs)

      case None =>
        response.setStatus(NOT_FOUND)
        HttpHeaders.setContentLength(response, 0)
    }
    respond(ctx, env)
  }

  /**
   * @return None if uri is invalid or the corresponding file is hidden,
   *         otherwise Some(the absolute file path)
   */
  private def sanitizeUri(uri: String): Option[String] = {
    // uri starts with "/"

    var decoded: String = null

    URLDecoder.decode(uri) match {
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
