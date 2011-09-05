package xitrum.handler.up

import org.jboss.netty.channel.{ChannelHandler, ChannelHandlerContext, MessageEvent, SimpleChannelUpstreamHandler}
import org.jboss.netty.handler.codec.http.{DefaultHttpResponse, HttpHeaders, HttpRequest, HttpResponseStatus, HttpVersion}
import org.jboss.netty.buffer.ChannelBuffers
import ChannelHandler.Sharable
import HttpHeaders.Names._
import HttpResponseStatus._
import HttpVersion._

import xitrum.Config
import xitrum.handler.{BaseUri, NotModified}
import xitrum.handler.updown.XSendfile
import xitrum.util.{Gzip, Mime, Loader, PathSanitizer}

@Sharable
class PublicResourceServer extends SimpleChannelUpstreamHandler with BadClientSilencer {
  override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {
    val m = e.getMessage
    if (!m.isInstanceOf[HttpRequest]) {
      ctx.sendUpstream(e)
      return
    }

    val request = m.asInstanceOf[HttpRequest]

    val pathInfo0 = request.getUri.split('?')(0)
    val pathInfo1 = BaseUri.remove(pathInfo0).get  // None has been checked at PublicFileServer

    if (!pathInfo1.startsWith("/resources/public/")) {
      ctx.sendUpstream(e)
      return
    }

    val path = pathInfo1.substring("/resources/".length)  // Remove "/resources/" leading
    loadResource(path) match {
      case None =>
        val response = new DefaultHttpResponse(HTTP_1_1, NOT_FOUND)
        XSendfile.set404Page(response)
        ctx.getChannel.write(response)

      case Some(bytes) =>
        // Size comparison is good enough
        // Cannot use web server startup time, because there may be multiple
        // web servers behind a load balancer!
        val length       = bytes.length
        val lastModified = NotModified.formatRfc2822(length * 10000000)  // Magnify the change in size
        val ims          = request.getHeader(IF_MODIFIED_SINCE)
        if (ims != null && ims == lastModified) {
          val response = new DefaultHttpResponse(HTTP_1_1, NOT_MODIFIED)
          HttpHeaders.setContentLength(response, 0)
          ctx.getChannel.write(response)
        } else {
          val response = new DefaultHttpResponse(HTTP_1_1, OK)
          response.setHeader(LAST_MODIFIED, lastModified)
          val mimeo = Mime.get(path)
          val bytes2 =
            if (mimeo.isDefined) {
              val mime = mimeo.get
              response.setHeader(CONTENT_TYPE, mime)

              val bl = bytes.length
              if (bl > Config.compressBigTextualResponseMinSizeInKB * 1024 && Mime.isTextual(mime)) {
                val ret = Gzip.compress(bytes)
                response.setHeader(CONTENT_ENCODING, "gzip")
                ret
              } else {
                bytes
              }
            } else {
              bytes
            }
          HttpHeaders.setContentLength(response, bytes2.length)
          response.setContent(ChannelBuffers.wrappedBuffer(bytes2))
          ctx.getChannel.write(response)
        }
    }
  }

  //----------------------------------------------------------------------------

  /**
   * Read whole file to memory. It's OK because the files are normally small.
   * No one is stupid enough to store large files in resources.
   *
   * @param path Relative to one of the elements in CLASSPATH, without leading "/"
   */
  private def loadResource(path: String): Option[Array[Byte]] = {
    PathSanitizer.sanitize(path) match {
      case None =>
        None

      case Some(path2) =>
        // http://www.javaworld.com/javaworld/javaqa/2003-08/01-qa-0808-property.html?page=2
        val stream = getClass.getClassLoader.getResourceAsStream(path2)

        if (stream == null) {
          None
        } else {
          val bytes = Loader.bytesFromInputStream(stream)
          Some(bytes)
        }
    }
  }
}
