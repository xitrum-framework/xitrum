package xitrum.handler.updown

import java.io.{File, RandomAccessFile}

import org.jboss.netty.channel.{ChannelEvent, ChannelUpstreamHandler, ChannelDownstreamHandler, Channels, ChannelHandlerContext, DownstreamMessageEvent, UpstreamMessageEvent, ChannelFuture, DefaultFileRegion, ChannelFutureListener}
import org.jboss.netty.handler.codec.http.{HttpHeaders, HttpRequest, HttpResponse, HttpResponseStatus, HttpVersion}
import HttpResponseStatus._
import HttpVersion._
import HttpHeaders.Names._
import HttpHeaders.Values._
import org.jboss.netty.handler.ssl.SslHandler
import org.jboss.netty.handler.stream.ChunkedFile
import org.jboss.netty.buffer.ChannelBuffers

import xitrum.{Config, Logger}
import xitrum.etag.{Etag, NotModified}
import xitrum.util.Mime

object XSendResource extends Logger {
  val CHUNK_SIZE = 8 * 1024

  private val X_SENDRESOURCE_HEADER = "X-Sendresource"

  def sendFile(ctx: ChannelHandlerContext, e: ChannelEvent, request: HttpRequest, response: HttpResponse, abs: String) {
  }
}

/**
 * This handler sends resource file (should be small) in CLASSPATH.
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
    if (!response.containsHeader(X_SENDRESOURCE_HEADER)) {
      ctx.sendDownstream(e)
      return
    }

    // X-Sendfile is not standard
    // To avoid leaking the information, we remove it
    val abs = response.getHeader(X_SENDRESOURCE_HEADER)
    response.removeHeader(X_SENDRESOURCE_HEADER)

    sendFile(ctx, e, request, response, abs)
  }
}
