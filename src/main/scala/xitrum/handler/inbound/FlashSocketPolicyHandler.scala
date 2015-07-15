package xitrum.handler.inbound

import io.netty.buffer.{ByteBuf, Unpooled}
import io.netty.channel.{ChannelFutureListener, ChannelHandlerContext, SimpleChannelInboundHandler}
import io.netty.util.CharsetUtil

import xitrum.handler.AccessLog
import xitrum.util.Loader

// http://www.adobe.com/devnet/flashplayer/articles/socket_policy_files.html
object FlashSocketPolicyHandler {
  // The request must be exactly "<policy-file-request/>\0"
  // To test:
  // perl -e 'printf "<policy-file-request/>%c",0' | nc localhost 8000
  val REQUEST        = Unpooled.copiedBuffer("<policy-file-request/>\u0000", CharsetUtil.UTF_8)
  val REQUEST_LENGTH = REQUEST.readableBytes

  val RESPONSE = Unpooled.wrappedBuffer(Loader.bytesFromClasspath("flash_socket_policy.xml"))
}

class FlashSocketPolicyHandler extends SimpleChannelInboundHandler[ByteBuf] {
  import FlashSocketPolicyHandler._

  private var nextIdx = 0

  override def channelRead0(ctx: ChannelHandlerContext, msg: ByteBuf) {
    if (msg.readableBytes + nextIdx > REQUEST_LENGTH) {
      sendUpstream(ctx, msg)
      return
    }

    if (!contains(msg, nextIdx)) {
      sendUpstream(ctx, msg)
      return
    }

    nextIdx += msg.readableBytes
    if (nextIdx != REQUEST_LENGTH) return

    // Respond

    // Some handlers may incorrectly intercept the response, remove all handlers
    // to avoid problem
    val pipeline = ctx.pipeline
    val it       = pipeline.iterator()
    while (it.hasNext()) {
      val entry   = it.next()
      val handler = entry.getValue
      pipeline.remove(handler)
    }

    // Respond and close
    val channel = ctx.channel
    channel.writeAndFlush(RESPONSE.duplicate().retain()).addListener(ChannelFutureListener.CLOSE)
    AccessLog.logFlashSocketPolicyFileAccess(channel.remoteAddress)
  }

  private def sendUpstream(ctx: ChannelHandlerContext, msg: ByteBuf) {
    if (nextIdx == 0)
      ctx.fireChannelRead(msg.retain())
    else
      ctx.fireChannelRead(Unpooled.wrappedBuffer(REQUEST.slice(0, nextIdx).retain(), msg.retain()))

    ctx.pipeline.remove(this)
  }

  private def contains(msg: ByteBuf, nextIdx: Int): Boolean = {
    val tmpl = REQUEST.slice(nextIdx, msg.readableBytes)
    tmpl.equals(msg)
  }
}
