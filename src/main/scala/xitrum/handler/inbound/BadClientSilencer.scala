package xitrum.handler.inbound

import io.netty.buffer.Unpooled
import io.netty.channel.{Channel, ChannelHandler, ChannelHandlerContext, ChannelFutureListener, SimpleChannelInboundHandler}
import ChannelHandler.Sharable
import io.netty.handler.codec.http.{DefaultFullHttpResponse, HttpVersion, HttpResponseStatus}
import xitrum.{Config, Log}

object BadClientSilencer {
  def respond400(channel: Channel, body: String) {
    val content  = Unpooled.copiedBuffer(body, Config.xitrum.request.charset)
    val response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST, content)
    channel.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
  }
}

/**
 * This handler should be put at the last position of the inbound pipeline to
 * catch all exception caused by bad client (closed connection, malformed request etc.).
 */
@Sharable
class BadClientSilencer extends SimpleChannelInboundHandler[Any] {
  override def channelRead0(ctx: ChannelHandlerContext, msg: Any) {
    // msg has not been handled by any previous handler
    ctx.channel.close()
    Log.trace("Unknown msg: " + msg)
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, e: Throwable) {
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
