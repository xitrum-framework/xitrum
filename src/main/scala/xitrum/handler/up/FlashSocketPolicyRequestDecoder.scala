package xitrum.handler.up

import org.jboss.netty.buffer.{ChannelBuffer, ChannelBuffers}
import org.jboss.netty.channel.{Channel, ChannelHandlerContext}
import org.jboss.netty.handler.codec.frame.FrameDecoder

object FlashSocketPolicyRequestDecoder {
  // The request must be exactly "<policy-file-request/>\0"
  val REQUEST                = ChannelBuffers.wrappedBuffer("<policy-file-request/>\0".getBytes)
  val REQUEST_LENGTH         = "<policy-file-request/>\0".length
  val TICKET_TO_NEXT_HANDLER = new Object
}

// See the Netty Guide
class FlashSocketPolicyRequestDecoder extends FrameDecoder {
  import FlashSocketPolicyRequestDecoder._

  protected override def decode(ctx: ChannelHandlerContext, channel: Channel, buffer: ChannelBuffer): Object = {
    if (buffer.readableBytes() < REQUEST_LENGTH) {
      null
    } else {
      // Check if the request is exactly "<policy-file-request/>\0"
      val channelBuffer = buffer.readBytes(REQUEST_LENGTH)
      if (channelBuffer.equals(REQUEST)) {
        TICKET_TO_NEXT_HANDLER
      } else {
        channel.close()
        null
      }
    }
  }
}
