package xitrum.handler.inbound

import io.netty.buffer.Unpooled
import io.netty.channel.{Channel, ChannelHandler, ChannelHandlerContext, ChannelFutureListener, SimpleChannelInboundHandler}
import ChannelHandler.Sharable
import io.netty.handler.codec.http.{DefaultFullHttpResponse, HttpVersion, HttpResponseStatus, HttpUtil}
import xitrum.{Config, Log}

object BadClientSilencer {
  /**
   * Responds 400 Bad Request to client.
   *
   * For security, do not expose server internal info to client via response content body.
   * Because the problem is caused by bad client, generally do not log at INFO or WARN level
   * to avoid noise in server log.
   */
  def respond400(channel: Channel, body: String): Unit = {
    val content = Unpooled.copiedBuffer(body, Config.xitrum.request.charset)

    // https://github.com/xitrum-framework/xitrum/issues/508#issuecomment-72808997
    // Do not close channel without responding status code.
    // Nginx decides that upstream is down if upstream drop connection without responding status code.
    val response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST, content)

    HttpUtil.setContentLength(response, content.readableBytes)
    channel.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE)
  }
}

/**
 * This handler should be put at the last position of the inbound pipeline to
 * catch all exception caused by bad client (closed connection, malformed request etc.).
 */
@Sharable
class BadClientSilencer extends SimpleChannelInboundHandler[Any] {
  override def channelRead0(ctx: ChannelHandlerContext, msg: Any): Unit = {
    // msg has not been handled by any previous handler
    ctx.channel.close()
    Log.trace("Unknown msg: " + msg)
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, e: Throwable): Unit = {
    ctx.channel.close()
    if (e.isInstanceOf[java.io.IOException]                            ||  // Connection reset by peer, Broken pipe
        e.isInstanceOf[java.nio.channels.ClosedChannelException]       ||
        e.isInstanceOf[io.netty.handler.codec.DecoderException]        ||
        e.isInstanceOf[io.netty.handler.codec.CorruptedFrameException] ||  // Bad WebSocket frame
        e.isInstanceOf[java.lang.IllegalArgumentException]             ||  // Use https://... URL to connect to HTTP server
        e.isInstanceOf[javax.net.ssl.SSLException]                     ||  // Use http://... URL to connect to HTTPS server
        e.isInstanceOf[io.netty.handler.ssl.NotSslRecordException])
      Log.trace("Caught exception", e)  // Maybe client is bad
    else
      Log.warn("Caught exception", e)   // Maybe server is bad
  }
}
