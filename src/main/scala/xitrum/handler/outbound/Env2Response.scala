package xitrum.handler.outbound

import io.netty.buffer.Unpooled
import io.netty.channel.{ChannelHandler, ChannelHandlerContext, ChannelOutboundHandlerAdapter, ChannelPromise}
import io.netty.handler.codec.http.{DefaultHttpResponse, HttpHeaders, HttpMethod, HttpRequest, FullHttpResponse, HttpResponseStatus, HttpVersion}
import ChannelHandler.Sharable
import HttpHeaders.Names._
import HttpMethod._
import HttpResponseStatus._

import xitrum.{Config, Log}
import xitrum.etag.Etag
import xitrum.handler.HandlerEnv
import xitrum.util.{ByteBufToBytes, Gzip, Mime}

@Sharable
class Env2Response extends ChannelOutboundHandlerAdapter with Log {
  override def write(ctx: ChannelHandlerContext, msg: Object, promise: ChannelPromise) {
    if (!msg.isInstanceOf[HandlerEnv]) {
      ctx.write(msg, promise)
      return
    }

    val env      = msg.asInstanceOf[HandlerEnv]
    val request  = env.request
    val response = env.response

    // Content-Length header may be set to > 0 even when the content is empty,
    // ex: HEAD or OPTIONS response
    if (!HttpHeaders.isContentLengthSet(response))
      HttpHeaders.setContentLength(response, response.content.readableBytes)

    if ((request.getMethod == HEAD || request.getMethod == OPTIONS) && response.getStatus == OK)
      // http://stackoverflow.com/questions/3854842/content-length-header-with-head-requests
      response.content.clear()
    else if (!tryEtag(request, response))
      Gzip.tryCompressBigTextualResponse(request, response)

    // For chunked response, we can't send "response" because it's a FullHttpResponse,
    // we need to send HttpResponse.
    if (HttpHeaders.isTransferEncodingChunked(response)) {
      val onlyHeaders = new DefaultHttpResponse(response.getProtocolVersion, response.getStatus)
      onlyHeaders.headers.set(response.headers)

      // TRANSFER_ENCODING header is not allowed in HTTP/1.0 response:
      // http://sockjs.github.com/sockjs-protocol/sockjs-protocol-0.3.3.html#section-165
      //
      // The header in the original response is a mark telling the response is chunked.
      // It should not be removed from the original response.
      if (request.getProtocolVersion.compareTo(HttpVersion.HTTP_1_0) == 0)
        HttpHeaders.removeTransferEncodingChunked(onlyHeaders)

      ctx.write(onlyHeaders, promise)
    } else {
      // Need to retain because response will be released when env.release() is called below
      ctx.write(response.retain(), promise)
    }

    env.release()

    // See DefaultHttpChannelInitializer
    // This is the last Xitrum handler, log the response
    if (log.isTraceEnabled) log.trace(response.toString)

    // Keep alive, channel reading resuming/closing etc. are handled
    // by the code that sends the response (Responder#respond)
  }

  //----------------------------------------------------------------------------

  /**
   * This does not make the server faster, but decreases the response transmission
   * time through the network to the browser.
   *
   * Only effective for non-empty non-async dynamic response,
   * e.g. not for static file (has alredy been handled and does not go through
   * this handler) or X-SendFile response (empty dynamic response).
   *
   * If HttpHeaders.getContentLength(response) != response.content.readableBytes,
   * it is because the response is sent in async mode.
   *
   * @return true if the NO_MODIFIED response is set by this method
   */
  private def tryEtag(request: HttpRequest, response: FullHttpResponse): Boolean = {
    if (response.headers.contains(CACHE_CONTROL) &&
        HttpHeaders.getHeader(response, CACHE_CONTROL).toLowerCase.contains("no-cache"))
      return false

    if (response.getStatus != OK)
      return false

    val contentLengthInHeader = HttpHeaders.getContentLength(response, 0)
    val byteBuf               = response.content
    if (contentLengthInHeader == 0 || contentLengthInHeader != byteBuf.readableBytes) return false

    // No need to calculate ETag if it has been set, e.g. by the controller
    val etag1 = HttpHeaders.getHeader(response, ETAG)
    if (etag1 != null) {
      compareAndSetETag(request, response, etag1)
    } else {
      // It's not useful to calculate ETag for big response
      if (byteBuf.readableBytes > Config.xitrum.staticFile.maxSizeInBytesOfCachedFiles) return false

      val etag2 = Etag.forBytes(ByteBufToBytes(byteBuf))
      compareAndSetETag(request, response, etag2)
    }
  }

  private def compareAndSetETag(request: HttpRequest, response: FullHttpResponse, etag: String): Boolean = {
    if (Etag.areEtagsIdentical(request, etag)) {
      // Only send headers, the response content is set to empty
      // (decrease response transmission time)
      response.setStatus(NOT_MODIFIED)
      HttpHeaders.removeHeader(response, CONTENT_TYPE) // http://sockjs.github.com/sockjs-protocol/sockjs-protocol-0.3.3.html#section-25
      response.content.clear()
      true
    } else {
      Etag.set(response, etag)
      false
    }
  }
}
