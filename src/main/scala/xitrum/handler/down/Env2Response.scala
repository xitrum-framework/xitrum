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
import xitrum.handler.updown.{XSendFile, XSendResource}
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
      // Only effective for dynamic response, static file response has already been handled
      if (!tryEtag(request, response)) Gzip.tryCompressBigTextualResponse(request, response)

      // Do not handle keep alive if XSendFile or XSendResource is used
      // because it is handled by them in their own way
      if (!XSendFile.isHeaderSet(response) && !XSendResource.isHeaderSet(response) && !HttpHeaders.isKeepAlive(request))
        future.addListener(ChannelFutureListener.CLOSE)
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
    if (response.getStatus != OK) return false

    val channelBuffer = response.getContent
    val readableBytes = channelBuffer.readableBytes
    if (readableBytes > Config.config.response.smallStaticFileSizeInKB * 1024) return false

    val etag1 = response.getHeader(ETAG)
    val etag2 =
      if (etag1 != null) {
        etag1
      } else {
        val bytes = new Array[Byte](readableBytes)
        channelBuffer.readBytes(bytes)
        Etag.forBytes(bytes)
      }

    if (request.getHeader(IF_NONE_MATCH) == etag2) {
      // Only send headers, the content is empty
      response.setStatus(NOT_MODIFIED)
      HttpHeaders.setContentLength(response, 0)
      response.setContent(ChannelBuffers.EMPTY_BUFFER)
      true
    } else {
      response.setHeader(ETAG, etag2)

      // The response channel buffer is empty now because we have already read
      // everything out, we need to set it back
      channelBuffer.resetReaderIndex

      false
    }
  }
}
