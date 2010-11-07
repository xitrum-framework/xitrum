package xt.http_handler

import xt._

import java.io.File

import org.jboss.netty.channel.{ChannelHandlerContext, ChannelEvent, MessageEvent, Channel}
import org.jboss.netty.handler.codec.http.{HttpRequest, DefaultHttpResponse, HttpResponseStatus, HttpVersion}
import HttpResponseStatus._
import HttpVersion._

class PublicHandler extends RequestHandler {
  def handleRequest(ctx: ChannelHandlerContext, e: MessageEvent, request: HttpRequest) {
    val uri = request.getUri
    if (!uri.startsWith("/public")) {
      ctx.sendUpstream(e)
      return
    }

    val response = new DefaultHttpResponse(HTTP_1_1, OK)
    sanitizeUri(uri) match {
      case Some(abs) => response.setHeader("X-Sendfile", abs)
      case None      => response.setStatus(NOT_FOUND)
    }
    respond(ctx, request, response)
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
