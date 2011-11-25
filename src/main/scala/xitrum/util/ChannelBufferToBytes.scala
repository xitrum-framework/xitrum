package xitrum.util

import org.jboss.netty.buffer.ChannelBuffer

object ChannelBufferToBytes {
  def apply(buffer: ChannelBuffer): Array[Byte] = {
    if (buffer.hasArray) {
      buffer.array
    } else {
       val bytes = new Array[Byte](buffer.readableBytes)
       buffer.readBytes(bytes)
       buffer.resetReaderIndex  // The buffer may be reread later
       bytes
    }
  }
}
