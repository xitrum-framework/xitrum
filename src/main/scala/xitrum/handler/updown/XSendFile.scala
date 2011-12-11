package xitrum.handler.updown

import java.io.{File, RandomAccessFile}

import io.netty.channel.{ChannelEvent, ChannelUpstreamHandler, ChannelDownstreamHandler, Channels, ChannelHandlerContext, DownstreamMessageEvent, UpstreamMessageEvent, ChannelFuture, DefaultFileRegion, ChannelFutureListener}
import io.netty.handler.codec.http.{HttpHeaders, HttpRequest, HttpResponse, HttpResponseStatus, HttpVersion}
import HttpResponseStatus._
import HttpVersion._
import HttpHeaders.Names._
import HttpHeaders.Values._
import io.netty.handler.ssl.SslHandler
import io.netty.handler.stream.ChunkedFile
import io.netty.buffer.ChannelBuffers

import xitrum.{Config, Logger}
import xitrum.etag.{Etag, NotModified}
import xitrum.util.{Gzip, Mime}

object XSendFile extends Logger {
  // setClientCacheAggressively should be called at PublicFileServer, not
  // here because XSendFile may be used by applications which does not want
  // to clients to cache.

  val CHUNK_SIZE = 8 * 1024

  private val X_SENDFILE_HEADER = "X-Sendfile"

  private val abs404 = Config.root + "/public/404.html"
  private val abs500 = Config.root + "/public/500.html"

  /** @param path see Renderer#renderFile */
  def setHeader(response: HttpResponse, path: String) {
    response.setHeader(X_SENDFILE_HEADER, path)
    HttpHeaders.setContentLength(response, 0)  // Env2Response checks Content-Length
  }

  def isHeaderSet(response: HttpResponse) = response.containsHeader(X_SENDFILE_HEADER)

  def set404Page(response: HttpResponse) {
    response.setStatus(NOT_FOUND)
    setHeader(response, abs404)
  }

  def set500Page(response: HttpResponse) {
    response.setStatus(INTERNAL_SERVER_ERROR)
    setHeader(response, abs500)
  }

  /** @param path see Renderer#renderFile */
  def sendFile(ctx: ChannelHandlerContext, e: ChannelEvent, request: HttpRequest, response: HttpResponse, path: String) {
    // Try to serve from cache
    Etag.forFile(path, Gzip.isAccepted(request)) match {
      case Etag.NotFound =>
        response.setStatus(NOT_FOUND)
        NotModified.removeClientCache(response)

        if (path.startsWith(abs404)) {  // Even 404.html is not found!
          HttpHeaders.setContentLength(response, 0)
          ctx.sendDownstream(e)
          if (!HttpHeaders.isKeepAlive(request)) e.getFuture.addListener(ChannelFutureListener.CLOSE)
        } else {
          sendFile(ctx, e, request, response, abs404)
        }

      case Etag.Small(bytes, etag, mimeo, gzipped) =>
        if (request.getHeader(IF_NONE_MATCH) == etag) {
          response.setStatus(NOT_MODIFIED)
          HttpHeaders.setContentLength(response, 0)
          response.setContent(ChannelBuffers.EMPTY_BUFFER)
        } else {
          response.setHeader(ETAG, etag)
          if (mimeo.isDefined) response.setHeader(CONTENT_TYPE,     mimeo.get)
          if (gzipped)         response.setHeader(CONTENT_ENCODING, "gzip")

          HttpHeaders.setContentLength(response, bytes.length)
          response.setContent(ChannelBuffers.wrappedBuffer(bytes))
        }
        ctx.sendDownstream(e)

        // Keep alive
        if (!HttpHeaders.isKeepAlive(request)) e.getFuture.addListener(ChannelFutureListener.CLOSE)

      case Etag.TooBig(file) =>
        // LAST_MODIFIED is not reliable as ETAG when this is a cluster of web servers,
        // but it's still good to give it a try
        val lastModifiedRfc2822 = NotModified.formatRfc2822(file.lastModified)
        if (request.getHeader(IF_MODIFIED_SINCE) == lastModifiedRfc2822) {
          response.setStatus(NOT_MODIFIED)
          HttpHeaders.setContentLength(response, 0)
          response.setContent(ChannelBuffers.EMPTY_BUFFER)
          ctx.sendDownstream(e)
        } else {
          val mimeo = Mime.get(path)
          val raf   = new RandomAccessFile(path, "r")

          // Write the initial line and the header
          HttpHeaders.setContentLength(response, raf.length)
          response.setHeader(LAST_MODIFIED, lastModifiedRfc2822)
          if (mimeo.isDefined) response.setHeader(CONTENT_TYPE, mimeo.get)
          ctx.sendDownstream(e)

          // Write the content
          if (ctx.getPipeline.get(classOf[SslHandler]) != null) {
            // Cannot use zero-copy with HTTPS
            val future = Channels.write(ctx.getChannel, new ChunkedFile(raf, 0, raf.length, CHUNK_SIZE))
            future.addListener(new ChannelFutureListener {
              def operationComplete(f: ChannelFuture) {
                raf.close
              }
            })
            if (!HttpHeaders.isKeepAlive(request)) future.addListener(ChannelFutureListener.CLOSE)
          } else {
            // No encryption - use zero-copy
            val region = new DefaultFileRegion(raf.getChannel, 0, raf.length)

            // This will cause ClosedChannelException:
            // Channels.write(ctx, e.getFuture, region)

            val future = Channels.write(ctx.getChannel, region)
            future.addListener(new ChannelFutureListener {
              def operationComplete(f: ChannelFuture) {
                region.releaseExternalResources
                raf.close
              }
            })
            if (!HttpHeaders.isKeepAlive(request)) future.addListener(ChannelFutureListener.CLOSE)
          }
        }
    }
  }
}

/**
 * This handler sends file:
 * 1. If the file is big: use zero-copy for HTTP or chunking for HTTPS
 * 2. If the file is small: cache in memory and use normal response
 */
class XSendFile extends ChannelUpstreamHandler with ChannelDownstreamHandler {
  import XSendFile._

  var request: HttpRequest = _

  def handleUpstream(ctx: ChannelHandlerContext, e: ChannelEvent) {
    if (!e.isInstanceOf[UpstreamMessageEvent]) {
      ctx.sendUpstream(e)
      return
    }

    val m = e.asInstanceOf[UpstreamMessageEvent].getMessage
    if (!m.isInstanceOf[HttpRequest]) {
      ctx.sendUpstream(e)
      return
    }

    request = m.asInstanceOf[HttpRequest]
    ctx.sendUpstream(e)
  }

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
    val path      = response.getHeader(X_SENDFILE_HEADER)
    if (path == null) {
      ctx.sendDownstream(e)
      return
    }

    // X-SendFile is not standard, remove to avoid leaking information
    response.removeHeader(X_SENDFILE_HEADER)
    sendFile(ctx, e, request, response, path)
  }
}
