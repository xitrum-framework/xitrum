package xitrum.handler.up

import org.jboss.netty.buffer.ChannelBuffer
import org.jboss.netty.channel.{Channel, ChannelHandlerContext}
import org.jboss.netty.handler.codec.frame.FrameDecoder

object FlashSocketPolicyRequestDecoder {
  // The request must be exactly "<policy-file-request/>\0"
  val REQUEST                = "<policy-file-request/>\0".getBytes
  val REQUEST_LENGTH         = REQUEST.length
  val TICKET_TO_NEXT_HANDLER = new Object
}

// See the Netty Guide
class FlashSocketPolicyRequestDecoder extends FrameDecoder {
  import FlashSocketPolicyRequestDecoder._

  protected override def decode(ctx: ChannelHandlerContext, channel: Channel, buffer: ChannelBuffer): Object = {
    if (buffer.readableBytes() < REQUEST_LENGTH) {
      null
    } else {
      val channelBuffer = buffer.readBytes(REQUEST_LENGTH)

      // Check if the request is exactly "<policy-file-request/>\0"
      var i = 0
      while (i < REQUEST_LENGTH) {
        val expectedByte = REQUEST(i)
        val receivedByte = channelBuffer.readByte()
        if (receivedByte != expectedByte) {
          channel.close()
          return null
        }
        i += 1
      }
      TICKET_TO_NEXT_HANDLER
    }
  }
}
