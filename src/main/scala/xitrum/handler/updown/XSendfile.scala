package xitrum.handler.updown

import java.io.File
import java.io.RandomAccessFile

import org.jboss.netty.channel.{ChannelEvent, ChannelUpstreamHandler, ChannelDownstreamHandler, Channels, ChannelHandlerContext, DownstreamMessageEvent, UpstreamMessageEvent, ChannelFuture, DefaultFileRegion, ChannelFutureListener}
import org.jboss.netty.handler.codec.http.{HttpHeaders, HttpRequest, HttpResponse, HttpResponseStatus, HttpVersion}
import HttpResponseStatus._
import HttpVersion._
import HttpHeaders.Names._
import org.jboss.netty.handler.ssl.SslHandler
import org.jboss.netty.handler.stream.ChunkedFile
import org.jboss.netty.buffer.ChannelBuffers

import xitrum.{Config, Logger}
import xitrum.handler.{Env, SmallFileCache}

object XSendfile extends Logger {
  val CHUNK_SIZE = 8192

  private val XSENDFILE_HEADER = "X-Sendfile"

  private val abs404 = System.getProperty("user.dir") + "/static/404.html"
  private val abs500 = System.getProperty("user.dir") + "/static/500.html"

  def setHeader(response: HttpResponse, abs: String) {
    response.setHeader(XSENDFILE_HEADER, abs)
  }

  def set404Page(response: HttpResponse) {
    response.setStatus(NOT_FOUND)
    setHeader(response, abs404)
  }

  def set500Page(response: HttpResponse) {
    response.setStatus(INTERNAL_SERVER_ERROR)
    setHeader(response, abs500)
  }

  def sendFile(ctx: ChannelHandlerContext, e: ChannelEvent, request: HttpRequest, response: HttpResponse, abs: String) {
    // Try to serve from cache
    SmallFileCache.get(abs) match {
      case SmallFileCache.Hit(bytes, gzipped, lastModified, mimeo) =>
        if (response.getStatus == OK && request.getHeader(IF_MODIFIED_SINCE) == lastModified) {
          response.setStatus(NOT_MODIFIED)
        } else {
          logger.debug("Serve " + abs + " from cache")
          response.setContent(ChannelBuffers.wrappedBuffer(bytes))
          HttpHeaders.setContentLength(response, bytes.length)
          if (gzipped) response.setHeader(CONTENT_ENCODING, "gzip")
          response.setHeader(LAST_MODIFIED, lastModified)
          if (mimeo.isDefined) response.setHeader(CONTENT_TYPE, mimeo.get)
        }
        ctx.sendDownstream(e)

      case SmallFileCache.FileNotFound =>
        response.setStatus(NOT_FOUND)
        if (abs.startsWith(abs404)) {
          // Event 404.html is not found!
          HttpHeaders.setContentLength(response, 0)
          ctx.sendDownstream(e)
        } else {
          sendFile(ctx, e, request, response, abs404)
        }

      case SmallFileCache.FileTooBig(raf, fileLength, lastModified, mimeo) =>
        if (response.getStatus == OK && request.getHeader(IF_MODIFIED_SINCE) == lastModified) {
          response.setStatus(NOT_MODIFIED)
          ctx.sendDownstream(e)
        } else {
          // Write the initial line and the header
          HttpHeaders.setContentLength(response, fileLength)
          response.setHeader(LAST_MODIFIED, lastModified)
          if (mimeo.isDefined) response.setHeader(CONTENT_TYPE, mimeo.get)
          ctx.sendDownstream(e)

          // Write the content
          if (ctx.getPipeline.get(classOf[SslHandler]) != null) {
            // Cannot use zero-copy with HTTPS
            val future = Channels.write(ctx.getChannel, new ChunkedFile(raf, 0, fileLength, CHUNK_SIZE))
            future.addListener(new ChannelFutureListener {
              def operationComplete(f: ChannelFuture) {
                raf.close
              }
            })

            // Keep alive
            if (!HttpHeaders.isKeepAlive(request)) {
              future.addListener(ChannelFutureListener.CLOSE)
            }
          } else {
            // No encryption - use zero-copy
            val region = new DefaultFileRegion(raf.getChannel, 0, fileLength)

            // This will cause ClosedChannelException:
            // Channels.write(ctx, e.getFuture, region)

            val future = Channels.write(ctx.getChannel, region)
            future.addListener(new ChannelFutureListener {
              def operationComplete(f: ChannelFuture) {
                region.releaseExternalResources
                raf.close
              }
            })

            // Keep alive
            if (!HttpHeaders.isKeepAlive(request)) {
              future.addListener(ChannelFutureListener.CLOSE)
            }
          }
        }
    }
  }
}

/**
 * This handler send file to client using various strategies:
 * 1. If the file is big: use zero-copy for HTTP or chunking for HTTPS
 * 2. If the file is small: cache in memory and use normal response
 */
class XSendfile extends ChannelUpstreamHandler with ChannelDownstreamHandler {
  import XSendfile._

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
    if (!response.containsHeader(XSENDFILE_HEADER)) {
      ctx.sendDownstream(e)
      return
    }

    // X-Sendfile is not standard
    // To avoid leaking the information, we remove it
    val abs = response.getHeader(XSENDFILE_HEADER)
    response.removeHeader(XSENDFILE_HEADER)

    sendFile(ctx, e, request, response, abs)
  }
}
