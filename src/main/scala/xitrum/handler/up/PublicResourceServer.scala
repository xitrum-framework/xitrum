package xitrum.handler.up

import org.jboss.netty.channel.{ChannelHandler, ChannelHandlerContext, MessageEvent, SimpleChannelUpstreamHandler}
import org.jboss.netty.handler.codec.http.{DefaultHttpResponse, HttpHeaders, HttpRequest, HttpResponseStatus, HttpVersion}
import org.jboss.netty.buffer.ChannelBuffers
import ChannelHandler.Sharable
import HttpHeaders.Names._
import HttpResponseStatus._
import HttpVersion._

import xitrum.{Config, MimeType}
import xitrum.handler.SmallFileCache

@Sharable
class PublicResourceServer extends SimpleChannelUpstreamHandler with ClosedClientSilencer {
  override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {
    val m = e.getMessage
    if (!m.isInstanceOf[HttpRequest]) {
      ctx.sendUpstream(e)
      return
    }

    val request = m.asInstanceOf[HttpRequest]
    val uri     = request.getUri
    if (!uri.startsWith("/resources/public/")) {
      ctx.sendUpstream(e)
      return
    }

    val path = uri.substring("/resources".length)  // Remove /resources prefix

    loadFromResource(path) match {
      case None =>
        val response = new DefaultHttpResponse(HTTP_1_1, NOT_FOUND)
        HttpHeaders.setContentLength(response, 0)
        ctx.getChannel.write(response)

      case Some(bytes) =>
        // Size comparison is good enough
        // Cannot use web server startup time, because there may be multiple
        // web servers behind a load balancer!
        val length       = bytes.length
        val lastModified = SmallFileCache.lastModified(length * 10000)  // Magnify the change in size
        val ims          = request.getHeader(IF_MODIFIED_SINCE)
        if (ims != null && ims == length) {
          val response = new DefaultHttpResponse(HTTP_1_1, NOT_MODIFIED)
          HttpHeaders.setContentLength(response, 0)
          ctx.getChannel.write(response)
        } else {
          val response = new DefaultHttpResponse(HTTP_1_1, OK)
          HttpHeaders.setContentLength(response, length)
          response.setHeader(LAST_MODIFIED, lastModified)
          val mimeo = MimeType.pathToMime(path)
          if (mimeo.isDefined) response.setHeader(CONTENT_TYPE, mimeo.get)
          response.setContent(ChannelBuffers.wrappedBuffer(bytes))
          ctx.getChannel.write(response)
        }
    }
  }

  //----------------------------------------------------------------------------

  /**
   * Read whole file to memory. It's OK because the files are normally small.
   * No one is stupid enough to store large files in resources.
   */
  private def loadFromResource(path: String): Option[Array[Byte]] = {
    if (path.contains("/.")) {  // Simple sanitize
      None
    } else {
      val stream = getClass.getResourceAsStream(path)
      if (stream == null) {
        None
      } else {
        val len   = stream.available
        val bytes = new Array[Byte](len)

        // Read whole file
        var total = 0
        while (total < len) {
          val bytesRead = stream.read(bytes, total, len - total)
          total += bytesRead
        }

        stream.close
        Some(bytes)
      }
    }
  }
}
