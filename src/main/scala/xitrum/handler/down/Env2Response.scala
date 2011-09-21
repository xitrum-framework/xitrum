package xitrum.handler.down

import org.jboss.netty.buffer.ChannelBuffers
import org.jboss.netty.channel.{ChannelHandler, SimpleChannelDownstreamHandler, ChannelHandlerContext, MessageEvent, Channels, ChannelFutureListener}
import ChannelHandler.Sharable
import org.jboss.netty.handler.codec.http.{HttpHeaders, HttpRequest, HttpResponse}
import HttpHeaders.Names._

import xitrum.Config
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
      tryCompressBigTextualResponse(response)
      if (!HttpHeaders.isKeepAlive(request)) future.addListener(ChannelFutureListener.CLOSE)
    }

    Channels.write(ctx, future, response)
  }

  private def tryCompressBigTextualResponse(response: HttpResponse) {
    if (response.containsHeader(CONTENT_ENCODING)) return

    val channelBuffer = response.getContent
    val readableBytes = channelBuffer.readableBytes
    if (readableBytes < Config.bigTextualResponseSizeInKB * 1024) return

    val contentType = response.getHeader(CONTENT_TYPE)
    if (contentType == null || !Mime.isTextual(contentType)) return

    val bytes  = new Array[Byte](readableBytes)
    channelBuffer.readBytes(bytes)
    val bytes2 = Gzip.compress(bytes)

    response.setHeader(CONTENT_ENCODING, "gzip")
    HttpHeaders.setContentLength(response, bytes2.length)
    response.setContent(ChannelBuffers.wrappedBuffer(bytes2))
  }
}
