package xitrum.handler.inbound

import io.netty.buffer.{ByteBuf, Unpooled}
import io.netty.channel.{Channel, ChannelHandlerContext, SimpleChannelInboundHandler}

import xitrum.handler.AccessLog
import xitrum.util.Loader

object FlashSocketPolicyHandler {
  // http://www.adobe.com/devnet/flashplayer/articles/socket_policy_files.html
  // The request must be exactly "<policy-file-request/>\0"
  // python -c 'print "<policy-file-request/>%c" % 0' | nc 127.0.0.1 8000
  // perl -e 'printf "<policy-file-request/>%c",0' | nc 127.0.0.1 8000
  val REQUEST        = Unpooled.wrappedBuffer("<policy-file-request/>\0".getBytes)
  val REQUEST_LENGTH = REQUEST.readableBytes

  val RESPONSE = Unpooled.wrappedBuffer(Loader.bytesFromClasspath("flash_socket_policy.xml"))
}

// See the Netty Guide
class FlashSocketPolicyHandler extends SimpleChannelInboundHandler[ByteBuf] with BadClientSilencer {
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

    val channel = ctx.channel
    channel.write(RESPONSE)
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
    val tmpl = REQUEST.slice(nextIdx, msg.readableBytes).retain()
    tmpl.equals(msg)
  }
}
