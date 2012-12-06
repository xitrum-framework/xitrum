package xitrum.handler.down

import java.io.{File, RandomAccessFile}

import org.jboss.netty.channel.{ChannelEvent, ChannelDownstreamHandler, Channels, ChannelHandler, ChannelHandlerContext, DownstreamMessageEvent, UpstreamMessageEvent, ChannelFuture, DefaultFileRegion, ChannelFutureListener}
import org.jboss.netty.handler.codec.http.{HttpHeaders, HttpMethod, HttpRequest, HttpResponse, HttpResponseStatus, HttpVersion}
import org.jboss.netty.handler.ssl.SslHandler
import org.jboss.netty.handler.stream.ChunkedFile
import org.jboss.netty.buffer.ChannelBuffers

import ChannelHandler.Sharable
import HttpHeaders.Names._
import HttpHeaders.Values._
import HttpMethod._
import HttpResponseStatus._
import HttpVersion._

import xitrum.{Config, Logger}
import xitrum.etag.{Etag, NotModified}
import xitrum.handler.AccessLog
import xitrum.handler.up.NoPipelining
import xitrum.util.{Gzip, Mime}

object XSendFile extends Logger {
  // setClientCacheAggressively should be called at PublicFileServer, not
  // here because XSendFile may be used by applications which does not want
  // to clients to cache.

  val CHUNK_SIZE        = 8 * 1024
  val X_SENDFILE_HEADER = "X-Sendfile"

  // To avoid duplicate log like this when X_SENDFILE_HEADER is set by controller
  // GET /echo/ -> 404 (static)
  // GET /echo/ -> xitrum.sockjs.SockJsController#iframe, pathParams: {iframe: } -> 404, 1 [ms]
  val X_SENDFILE_HEADER_IS_FROM_CONTROLLER = "X-Sendfile-Is-From-Controller"

  private[this] val abs404 = Config.root + "/public/404.html"
  private[this] val abs500 = Config.root + "/public/500.html"

  /** @param path see Renderer#renderFile */
  def setHeader(response: HttpResponse, path: String, fromController: Boolean) {
    response.setHeader(X_SENDFILE_HEADER, path)
    if (fromController) response.setHeader(X_SENDFILE_HEADER_IS_FROM_CONTROLLER, "true")
    HttpHeaders.setContentLength(response, 0)  // Env2Response checks Content-Length
  }

  def isHeaderSet(response: HttpResponse) = response.containsHeader(X_SENDFILE_HEADER)

  def set404Page(response: HttpResponse, fromController: Boolean) {
    response.setStatus(NOT_FOUND)
    setHeader(response, abs404, fromController)
  }

  def set500Page(response: HttpResponse, fromController: Boolean) {
    response.setStatus(INTERNAL_SERVER_ERROR)
    setHeader(response, abs500, fromController)
  }

  /** @param path see Renderer#renderFile */
  def sendFile(ctx: ChannelHandlerContext, e: ChannelEvent, request: HttpRequest, response: HttpResponse, path: String, noLog: Boolean) {
    val channel       = ctx.getChannel
    val remoteAddress = channel.getRemoteAddress

    // Try to serve from cache
    Etag.forFile(path, Gzip.isAccepted(request)) match {
      case Etag.NotFound =>
        response.setStatus(NOT_FOUND)
        NotModified.setNoClientCache(response)

        if (path.startsWith(abs404)) {  // Even 404.html is not found!
          HttpHeaders.setContentLength(response, 0)
          NoPipelining.setResponseHeaderAndResumeReadingForKeepAliveRequestOrCloseOnComplete(request, response, channel, e.getFuture)
          ctx.sendDownstream(e)
          if (!noLog) AccessLog.logStaticFileAccess(remoteAddress, request, response)
        } else {
          sendFile(ctx, e, request, response, abs404, noLog)  // Recursive
        }

      case Etag.Small(bytes, etag, mimeo, gzipped) =>
        if (Etag.areEtagsIdentical(request, etag)) {
          response.setStatus(NOT_MODIFIED)
          HttpHeaders.setContentLength(response, 0)
          response.setContent(ChannelBuffers.EMPTY_BUFFER)
        } else {
          Etag.set(response, etag)
          if (mimeo.isDefined) response.setHeader(CONTENT_TYPE,     mimeo.get)
          if (gzipped)         response.setHeader(CONTENT_ENCODING, "gzip")

          HttpHeaders.setContentLength(response, bytes.length)
          if (request.getMethod == HttpMethod.HEAD && response.getStatus == OK)
            // http://stackoverflow.com/questions/3854842/content-length-header-with-head-requests
            response.setContent(ChannelBuffers.EMPTY_BUFFER)
          else
            response.setContent(ChannelBuffers.wrappedBuffer(bytes))
        }
        NoPipelining.setResponseHeaderAndResumeReadingForKeepAliveRequestOrCloseOnComplete(request, response, channel, e.getFuture)
        ctx.sendDownstream(e)
        if (!noLog) AccessLog.logStaticFileAccess(remoteAddress, request, response)

      case Etag.TooBig(file) =>
        // LAST_MODIFIED is not reliable as ETAG when this is a cluster of web servers,
        // but it's still good to give it a try
        val lastModifiedRfc2822 = NotModified.formatRfc2822(file.lastModified)
        if (request.getHeader(IF_MODIFIED_SINCE) == lastModifiedRfc2822) {
          response.setStatus(NOT_MODIFIED)
          HttpHeaders.setContentLength(response, 0)
          NoPipelining.setResponseHeaderAndResumeReadingForKeepAliveRequestOrCloseOnComplete(request, response, channel, e.getFuture)
          response.setContent(ChannelBuffers.EMPTY_BUFFER)
          ctx.sendDownstream(e)
          if (!noLog) AccessLog.logStaticFileAccess(remoteAddress, request, response)
        } else {
          val mimeo = Mime.get(path)
          val raf   = new RandomAccessFile(path, "r")

          val (offset, length) = getRangeFromRequest(request) match {
            case None =>
              (0L, raf.length)  // 0L is for avoiding "type mismatch" compile error
            case Some((startIndex, endIndex)) =>
              val endIndex2 = if (endIndex >= 0) endIndex else raf.length - 1
              response.setStatus(PARTIAL_CONTENT)
              response.setHeader(ACCEPT_RANGES, BYTES)
              response.setHeader(CONTENT_RANGE, "bytes " + startIndex + "-" + endIndex2 + "/" + raf.length)
              (startIndex, endIndex2 - startIndex + 1)
          }

          HttpHeaders.setContentLength(response, length)
          response.setHeader(LAST_MODIFIED, lastModifiedRfc2822)
          if (mimeo.isDefined) response.setHeader(CONTENT_TYPE, mimeo.get)
          NoPipelining.setResponseHeaderForKeepAliveRequest(request, response)
          if (!noLog) AccessLog.logStaticFileAccess(remoteAddress, request, response)

          // Write the content

          if (request.getMethod == HEAD && response.getStatus == OK) {
            // http://stackoverflow.com/questions/3854842/content-length-header-with-head-requests
            response.setContent(ChannelBuffers.EMPTY_BUFFER)
            ctx.sendDownstream(e)
            NoPipelining.if_keepAliveRequest_then_resumeReading_else_closeOnComplete(request, channel, e.getFuture)
          } else {
            // Send the initial line and headers
            ctx.sendDownstream(e)

            if (ctx.getPipeline.get(classOf[SslHandler]) != null) {
              // Cannot use zero-copy with HTTPS
              val future = Channels.write(channel, new ChunkedFile(raf, offset, length, CHUNK_SIZE))
              future.addListener(new ChannelFutureListener {
                def operationComplete(f: ChannelFuture) { raf.close() }
              })

              NoPipelining.if_keepAliveRequest_then_resumeReading_else_closeOnComplete(request, channel, future)
            } else {
              // No encryption - use zero-copy
              val region = new DefaultFileRegion(raf.getChannel, offset, length)

              // This will cause ClosedChannelException:
              // Channels.write(ctx, e.getFuture, region)

              val future = Channels.write(channel, region)
              future.addListener(new ChannelFutureListener {
                def operationComplete(f: ChannelFuture) {
                  region.releaseExternalResources()
                  raf.close()
                }
              })

              NoPipelining.if_keepAliveRequest_then_resumeReading_else_closeOnComplete(request, channel, future)
            }
          }
        }
    }
  }

  /**
   * "Range" request: http://tools.ietf.org/html/rfc2616#section-14.35
   * For simplicity only these specs are supported:
   * bytes=123-456
   * bytes=123-
   *
   * @return None or Some((start index, end index)), negative end index means file length - 1
   */
  private def getRangeFromRequest(request: HttpRequest): Option[(Long, Long)] = {
    val spec = request.getHeader(RANGE)

    try {
      if (spec == null) {
        None
      } else {
        if (spec.length <= 6) {
          None
        } else {
          val range = spec.substring(6)  // Skip "bytes="
          val se    = range.split('-')
          if (se.length == 2) {
            val s = se(0).toLong
            val e = se(1).toLong
            Some((s, e))
          } else if (se.length != 1) {
            None
          } else {
            val s = se(0).toLong
            val e = -1
            Some((s, e))
          }
        }
      }
    } catch {
      case e: Exception =>
        logger.warn("Unsupported Range spec: " + spec)
        None
    }
  }
}

/**
 * This handler sends file:
 * 1. If the file is big: use zero-copy for HTTP or chunking for HTTPS
 * 2. If the file is small: cache in memory and use normal response
 */
@Sharable
class XSendFile extends ChannelDownstreamHandler {
  import XSendFile._

  def handleDownstream(ctx: ChannelHandlerContext, e: ChannelEvent) {
    if (!e.isInstanceOf[DownstreamMessageEvent]) {
      ctx.sendDownstream(e)
      return
    }

    val m = e.asInstanceOf[DownstreamMessageEvent].getMessage
    if (!m.isInstanceOf[HttpResponse]) {
      ctx.sendDownstream(e)
      return
    }

    val response = m.asInstanceOf[HttpResponse]
    val path     = response.getHeader(X_SENDFILE_HEADER)
    if (path == null) {
      ctx.sendDownstream(e)
      return
    }

    // Remove non-standard header to avoid leaking information
    response.removeHeader(X_SENDFILE_HEADER)

    // See comment of X_SENDFILE_HEADER_IS_FROM_CONTROLLER
    // Remove non-standard header to avoid leaking information
    val noLog = response.containsHeader(X_SENDFILE_HEADER_IS_FROM_CONTROLLER)
    if (noLog) response.removeHeader(X_SENDFILE_HEADER_IS_FROM_CONTROLLER)

    val request = ctx.getChannel.getAttachment.asInstanceOf[HttpRequest]
    sendFile(ctx, e, request, response, path, noLog)
  }
}
