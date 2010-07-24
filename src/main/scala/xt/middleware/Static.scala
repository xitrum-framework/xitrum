package xt.middleware

import java.io.File
import java.io.RandomAccessFile
import java.net.URLDecoder
import java.io.UnsupportedEncodingException

import scala.collection.mutable.Map

import org.jboss.netty.channel.{Channel, ChannelFuture, DefaultFileRegion,
                                ChannelFutureProgressListener, ChannelFutureListener}
import org.jboss.netty.handler.codec.http.{HttpRequest, HttpResponse,
                                           HttpResponseStatus, HttpHeaders}
import org.jboss.netty.handler.ssl.SslHandler
import org.jboss.netty.handler.stream.ChunkedFile

/**
 * Serves static files inside static directory.
 */
object Static {
  def wrap(app: App) = new App {
    def call(channel: Channel, request: HttpRequest, response: HttpResponse, env: Map[String, Any]) {
      val uri = request.getUri
      if (!uri.startsWith("/static"))
        app.call(channel, request, response, env)
      else {
        sanitizeUri(uri) match {
          case None =>
            response.setStatus(HttpResponseStatus.NOT_FOUND)

          case Some(abs) =>
            // Check
            val file = new File(abs)
            if (file.isHidden() || !file.exists()) {
              response.setStatus(HttpResponseStatus.NOT_FOUND)
              return
            }

            if (!file.isFile()) {
              response.setStatus(HttpResponseStatus.NOT_FOUND)
              return
            }

            // Check
            var raf: RandomAccessFile = null
            try {
              raf = new RandomAccessFile(file, "r")
            } catch {
              case _ =>
                response.setStatus(HttpResponseStatus.NOT_FOUND)
                return
            }

            // Write the initial line and the header
            val fileLength = raf.length
            HttpHeaders.setContentLength(response, fileLength)
            channel.write(response)

            // Write the content
            var writeFuture: ChannelFuture = null
            if (channel.getPipeline.get(classOf[SslHandler]) != null) {
              // Cannot use zero-copy with HTTPS
              writeFuture = channel.write(new ChunkedFile(raf, 0, fileLength, 8192));
            } else {
              // No encryption - use zero-copy
              val region = new DefaultFileRegion(raf.getChannel(), 0, fileLength)
              writeFuture = channel.write(region)
              writeFuture.addListener(new ChannelFutureProgressListener {
                def operationComplete(future: ChannelFuture) {
                  region.releaseExternalResources
                }

                def operationProgressed(future: ChannelFuture, amount: Long, current: Long, total: Long) {}
              })
            }

            // Decide whether to close the connection or not.
            if (!HttpHeaders.isKeepAlive(request)) {
              // Close the connection when the whole content is written out.
              writeFuture.addListener(ChannelFutureListener.CLOSE)
            }

            // The Netty handler should not do anything with the response
            env.put("ignore_response", true)
        }
      }
    }
  }

  /**
   * @return None if the URI is invalid
   */
  private def sanitizeUri(uri: String): Option[String] = {
    var ret: String = null

    // Decode the path
    try {
      ret = URLDecoder.decode(uri, "UTF-8")
    } catch {
      case e: UnsupportedEncodingException =>
        try {
          ret = URLDecoder.decode(uri, "ISO-8859-1")
        } catch {
          case _ => return None
        }
    }

    // Convert file separators
    ret = ret.replace('/', File.separatorChar)

    // Simplistic dumb security check
    if (ret.contains(File.separator + ".") ||
        ret.contains("." + File.separator) ||
        ret.startsWith(".")                ||
        ret.endsWith(".")) {
      return None
    }

    // Convert to absolute path
    // user.dir: User's current working directory
    // See: http://www.java2s.com/Tutorial/Java/0120__Development/Javasystemproperties.htm
    val abs = System.getProperty("user.dir") + ret

    Some(abs)
  }
}
