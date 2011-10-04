package xitrum.handler.down

import org.jboss.netty.buffer.ChannelBuffers
import org.jboss.netty.channel.{ChannelHandler, SimpleChannelDownstreamHandler, ChannelHandlerContext, MessageEvent, Channels, ChannelFutureListener}
import ChannelHandler.Sharable
import org.jboss.netty.handler.codec.http.{HttpHeaders, HttpRequest, HttpResponse, HttpResponseStatus}
import HttpHeaders.Names._
import HttpResponseStatus._

import xitrum.Config
import xitrum.etag.Etag
import xitrum.handler.HandlerEnv
import xitrum.util.{Gzip, Mime}

@Sharable
class Env2Response extends SimpleChannelDownstreamHandler {
  override def writeRequested(ctx: ChannelHandlerContext, e: MessageEvent) {
    val m = e.getMessage
    if (!m.isInstanceOf[HandlerEnv]) {
      ctx.sendDownstream(e)
      return
    }

    val env      = m.asInstanceOf[HandlerEnv]
    val request  = env.request
    val response = env.response
    val future   = e.getFuture

    // If HttpHeaders.getContentLength(response) > response.getContent.readableBytes,
    // it is because the response body will be sent later and the channel will
    // be closed later by the code that sends the response body
    if (HttpHeaders.getContentLength(response) == response.getContent.readableBytes) {
      // Only effective for dynamic response, static file response
      // has already been handled
      if (!tryEtag(request, response)) tryCompressBigTextualResponse(request, response)

      if (!HttpHeaders.isKeepAlive(request)) future.addListener(ChannelFutureListener.CLOSE)
    }

    Channels.write(ctx, future, response)
  }

  //----------------------------------------------------------------------------

 /**
   * This does not make the server faster, but decrease the response transmission
   * time through the network to the browser.
   *
   * @return true if the NO_MODIFIED response is set by this method
   */
  private def tryEtag(request: HttpRequest, response: HttpResponse): Boolean = {
    if (response.containsHeader(ETAG)) return false

    val readableBytes = response.getContent.readableBytes
    if (readableBytes > Config.smallStaticFileSizeInKB * 1024) return false

    val channelBuffer = response.getContent
    val bytes = new Array[Byte](readableBytes)
    channelBuffer.readBytes(bytes)
    val etag = Etag.forBytes(bytes)

    if (request.getHeader(IF_NONE_MATCH) == etag) {
      // Only send headers, the content is empty
      response.setStatus(NOT_MODIFIED)
      HttpHeaders.setContentLength(response, 0)
      response.setContent(ChannelBuffers.EMPTY_BUFFER)
      true
    } else {
      response.setHeader(ETAG, etag)

      // The response channel buffer is empty now because we have already read
      // everything out, we need to set it back
      response.setContent(ChannelBuffers.copiedBuffer(bytes))

      false
    }
  }

  private def tryCompressBigTextualResponse(request: HttpRequest, response: HttpResponse) {
    if (!Gzip.isAccepted(request) || response.containsHeader(CONTENT_ENCODING)) return

    val contentType = response.getHeader(CONTENT_TYPE)
    if (contentType == null || !Mime.isTextual(contentType)) return

    val channelBuffer = response.getContent
    val readableBytes = channelBuffer.readableBytes
    if (readableBytes < Config.BIG_TEXTUAL_RESPONSE_SIZE_IN_KB * 1024) return

    val bytes  = new Array[Byte](readableBytes)
    channelBuffer.readBytes(bytes)
    val bytes2 = Gzip.compress(bytes)

    response.setHeader(CONTENT_ENCODING, "gzip")
    HttpHeaders.setContentLength(response, bytes2.length)
    response.setContent(ChannelBuffers.wrappedBuffer(bytes2))
  }
}
