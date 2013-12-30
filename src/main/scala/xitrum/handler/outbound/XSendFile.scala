package xitrum.handler.outbound

import java.io.{File, RandomAccessFile}
import scala.util.control.NonFatal
import io.netty.channel.{ChannelOutboundHandlerAdapter, ChannelHandler, ChannelHandlerContext, ChannelFuture, ChannelPromise, DefaultFileRegion, ChannelFutureListener}
import io.netty.handler.codec.http.{HttpHeaders, HttpMethod, FullHttpRequest, FullHttpResponse, HttpResponseStatus, HttpVersion}
import io.netty.handler.ssl.SslHandler
import io.netty.handler.stream.ChunkedFile
import ChannelHandler.Sharable
import HttpHeaders.Names._
import HttpHeaders.Values._
import HttpMethod._
import HttpResponseStatus._
import HttpVersion._
import xitrum.{Config, Log}
import xitrum.etag.{Etag, NotModified}
import xitrum.handler.{AccessLog, HandlerEnv}
import xitrum.handler.inbound.NoPipelining
import xitrum.util.{Gzip, Mime}
import io.netty.buffer.Unpooled

object XSendFile extends Log {
  // setClientCacheAggressively should be called at PublicFileServer, not
  // here because XSendFile may be used by applications which does not want
  // to clients to cache.

  val CHUNK_SIZE        = 8 * 1024
  val X_SENDFILE_HEADER = "X-Sendfile"

  // To avoid duplicate log like this when X_SENDFILE_HEADER is set by action
  // GET /echo/ -> 404 (static)
  // GET /echo/ -> xitrum.sockjs.SockJsController#iframe, pathParams: {iframe: } -> 404, 1 [ms]
  val X_SENDFILE_HEADER_IS_FROM_ACTION = "X-Sendfile-Is-From-Action"

  private[this] val abs404 = Config.root + "/public/404.html"
  private[this] val abs500 = Config.root + "/public/500.html"

  /** @param path see Renderer#renderFile */
  def setHeader(response: FullHttpResponse, path: String, fromAction: Boolean) {
    HttpHeaders.setHeader(response, X_SENDFILE_HEADER, path)
    if (fromAction) HttpHeaders.setHeader(response, X_SENDFILE_HEADER_IS_FROM_ACTION, "true")
    HttpHeaders.setContentLength(response, 0)  // Env2Response checks Content-Length
  }

  def isHeaderSet(response: FullHttpResponse) = response.headers.contains(X_SENDFILE_HEADER)

  def set404Page(response: FullHttpResponse, fromController: Boolean) {
    response.setStatus(NOT_FOUND)
    setHeader(response, abs404, fromController)
  }

  def set500Page(response: FullHttpResponse, fromController: Boolean) {
    response.setStatus(INTERNAL_SERVER_ERROR)
    setHeader(response, abs500, fromController)
  }

  /** @param path see Renderer#renderFile */
  def sendFile(
      ctx: ChannelHandlerContext, msg: Any, promise: ChannelPromise,
      request: FullHttpRequest, response: FullHttpResponse, path: String, noLog: Boolean)
  {
    val channel       = ctx.channel
    val remoteAddress = channel.remoteAddress

    // Try to serve from cache
    Etag.forFile(path, Gzip.isAccepted(request)) match {
      case Etag.NotFound =>
        response.setStatus(NOT_FOUND)
        NotModified.setNoClientCache(response)

        if (path.startsWith(abs404)) {  // Even 404.html is not found!
          HttpHeaders.setContentLength(response, 0)
          NoPipelining.setResponseHeaderAndResumeReadingForKeepAliveRequestOrCloseOnComplete(request, response, channel, promise)
          ctx.write(msg, promise)
          if (!noLog) AccessLog.logStaticFileAccess(remoteAddress, request, response)
        } else {
          sendFile(ctx, msg, promise, request, response, abs404, noLog)  // Recursive
        }

      case Etag.Small(bytes, etag, mimeo, gzipped) =>
        if (Etag.areEtagsIdentical(request, etag)) {
          response.setStatus(NOT_MODIFIED)
          HttpHeaders.setContentLength(response, 0)
          response.content.clear()
        } else {
          Etag.set(response, etag)
          if (mimeo.isDefined) HttpHeaders.setHeader(response, CONTENT_TYPE,     mimeo.get)
          if (gzipped)         HttpHeaders.setHeader(response, CONTENT_ENCODING, "gzip")

          HttpHeaders.setContentLength(response, bytes.length)
          if ((request.getMethod == HEAD || request.getMethod == OPTIONS) && response.getStatus == OK) {
            // http://stackoverflow.com/questions/3854842/content-length-header-with-head-requests
            response.content.clear()
          } else {
            response.content.clear()
            response.content.writeBytes(Unpooled.wrappedBuffer(bytes))
          }
        }
        NoPipelining.setResponseHeaderAndResumeReadingForKeepAliveRequestOrCloseOnComplete(request, response, channel, promise)
        ctx.write(msg, promise)
        if (!noLog) AccessLog.logStaticFileAccess(remoteAddress, request, response)

      case Etag.TooBig(file) =>
        // LAST_MODIFIED is not reliable as ETAG when this is a cluster of web servers,
        // but it's still good to give it a try
        val lastModifiedRfc2822 = NotModified.formatRfc2822(file.lastModified)
        if (HttpHeaders.getHeader(request, IF_MODIFIED_SINCE) == lastModifiedRfc2822) {
          response.setStatus(NOT_MODIFIED)
          HttpHeaders.setContentLength(response, 0)
          NoPipelining.setResponseHeaderAndResumeReadingForKeepAliveRequestOrCloseOnComplete(request, response, channel, promise)
          response.content.clear()
          ctx.write(msg, promise)
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
              HttpHeaders.setHeader(response, ACCEPT_RANGES, BYTES)
              HttpHeaders.setHeader(response, CONTENT_RANGE, "bytes " + startIndex + "-" + endIndex2 + "/" + raf.length)
              (startIndex, endIndex2 - startIndex + 1)
          }

          HttpHeaders.setContentLength(response, length)
          HttpHeaders.setHeader(response, LAST_MODIFIED, lastModifiedRfc2822)
          if (mimeo.isDefined) HttpHeaders.setHeader(response, CONTENT_TYPE, mimeo.get)
          NoPipelining.setResponseHeaderForKeepAliveRequest(request, response)
          if (!noLog) AccessLog.logStaticFileAccess(remoteAddress, request, response)

          // Write the content

          if (request.getMethod == HEAD && response.getStatus == OK) {
            // http://stackoverflow.com/questions/3854842/content-length-header-with-head-requests
            response.content.clear()
            ctx.write(msg, promise)
            NoPipelining.if_keepAliveRequest_then_resumeReading_else_closeOnComplete(request, channel, promise)
          } else {
            // Send the initial line and headers
            ctx.write(msg, promise)

            if (ctx.pipeline.get(classOf[SslHandler]) != null) {
              // Cannot use zero-copy with HTTPS
              val future = ctx.writeAndFlush(new ChunkedFile(raf, offset, length, CHUNK_SIZE), promise)
              future.addListener(new ChannelFutureListener {
                def operationComplete(f: ChannelFuture) { raf.close() }
              })

              NoPipelining.if_keepAliveRequest_then_resumeReading_else_closeOnComplete(request, channel, future)
            } else {
              // No encryption - use zero-copy
              val region = new DefaultFileRegion(raf.getChannel, offset, length)

              // This will cause ClosedChannelException:
              // Channels.write(ctx, e.getFuture, region)

              val future = ctx.writeAndFlush(region, promise)
              future.addListener(new ChannelFutureListener {
                def operationComplete(f: ChannelFuture) {
                  region.release()
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
  private def getRangeFromRequest(request: FullHttpRequest): Option[(Long, Long)] = {
    val spec = HttpHeaders.getHeader(request, RANGE)

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
      case NonFatal(e) =>
        log.warn("Unsupported Range spec: " + spec)
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
class XSendFile extends ChannelOutboundHandlerAdapter {
  import XSendFile._

  override def write(ctx: ChannelHandlerContext, msg: Any, promise: ChannelPromise) {
    if (!msg.isInstanceOf[HandlerEnv]) {
      ctx.write(msg, promise)
      return
    }

    val env      = msg.asInstanceOf[HandlerEnv]
    val response = env.response
    val path     = HttpHeaders.getHeader(response, X_SENDFILE_HEADER)
    if (path == null) {
      ctx.write(msg, promise)
      return
    }

    // Remove non-standard header to avoid leaking information
    HttpHeaders.removeHeader(response, X_SENDFILE_HEADER)

    // See comment of X_SENDFILE_HEADER_IS_FROM_CONTROLLER
    // Remove non-standard header to avoid leaking information
    val noLog = response.headers.contains(X_SENDFILE_HEADER_IS_FROM_ACTION)
    if (noLog) HttpHeaders.removeHeader(response, X_SENDFILE_HEADER_IS_FROM_ACTION)

    val request = env.request
    sendFile(ctx, msg: Any, promise, request, response, path, noLog)
  }
}
