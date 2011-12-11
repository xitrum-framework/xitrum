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

object XSendResource extends Logger {
  // setClientCacheAggressively should be called at PublicResourceServer, not
  // here because XSendResource may be used by applications which does not want
  // to clients to cache.

  val CHUNK_SIZE = 8 * 1024

  private val X_SENDRESOURCE_HEADER = "X-Sendresource"

  def setHeader(response: HttpResponse, path: String) {
    response.setHeader(X_SENDRESOURCE_HEADER, path)
  }

  def isHeaderSet(response: HttpResponse) = response.containsHeader(X_SENDRESOURCE_HEADER)

  /** @return false if not found */
  def sendResource(ctx: ChannelHandlerContext, e: ChannelEvent, request: HttpRequest, response: HttpResponse, path: String) {
    Etag.forResource(path, Gzip.isAccepted(request)) match {
      case Etag.NotFound =>
        // Keep alive is handled by XSendFile
        XSendFile.set404Page(response)

      case Etag.Small(bytes, etag, mimeo, gzipped) =>
        if (Etag.areEtagsIdentical(request, etag)) {
          response.setStatus(NOT_MODIFIED)
          HttpHeaders.setContentLength(response, 0)
          response.setContent(ChannelBuffers.EMPTY_BUFFER)
        } else {
          response.setHeader(ETAG, etag)
          if (mimeo.isDefined) response.setHeader(CONTENT_TYPE, mimeo.get)
          if (gzipped)         response.setHeader(CONTENT_ENCODING, "gzip")

          HttpHeaders.setContentLength(response, bytes.length)
          response.setContent(ChannelBuffers.wrappedBuffer(bytes))
        }

        if (!HttpHeaders.isKeepAlive(request)) e.getFuture.addListener(ChannelFutureListener.CLOSE)
    }
    ctx.sendDownstream(e)
  }
}

/**
 * This handler sends resource files (should be small) in classpath.
 */
class XSendResource extends ChannelUpstreamHandler with ChannelDownstreamHandler {
  import XSendResource._

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
    val path     = response.getHeader(X_SENDRESOURCE_HEADER)
    if (path == null) {
      ctx.sendDownstream(e)
      return
    }

    // X-SendResource is not standard, remove to avoid leaking information
    response.removeHeader(X_SENDRESOURCE_HEADER)
    sendResource(ctx, e, request, response, path)
  }
}
