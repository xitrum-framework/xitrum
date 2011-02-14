package xitrum.handler.up

import org.jboss.netty.channel.{ChannelHandler, ChannelHandlerContext, MessageEvent, SimpleChannelUpstreamHandler}
import org.jboss.netty.handler.codec.http.{DefaultHttpResponse, HttpHeaders, HttpRequest, HttpResponseStatus, HttpVersion}
import org.jboss.netty.buffer.ChannelBuffers
import ChannelHandler.Sharable
import HttpHeaders.Names._
import HttpResponseStatus._
import HttpVersion._

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
        val len  = bytes.length
        val lens = len.toString
        val ims  = request.getHeader(IF_MODIFIED_SINCE)
        if (ims != null && ims == lens) {
          val response = new DefaultHttpResponse(HTTP_1_1, NOT_MODIFIED)
          HttpHeaders.setContentLength(response, 0)
          ctx.getChannel.write(response)
        } else {
          val response = new DefaultHttpResponse(HTTP_1_1, OK)
          HttpHeaders.setContentLength(response, len)
          response.setHeader(LAST_MODIFIED, lens)
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
