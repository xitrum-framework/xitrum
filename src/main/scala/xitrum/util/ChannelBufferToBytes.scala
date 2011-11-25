package xitrum.util

import org.jboss.netty.buffer.ChannelBuffer

object ChannelBufferToBytes {
  def apply(buffer: ChannelBuffer): Array[Byte] = {
    val len = buffer.readableBytes
    val ret = new Array[Byte](len)
    if (buffer.hasArray) {
      // https://github.com/netty/netty/issues/83
      System.arraycopy(buffer.array, 0, ret, 0, len)
    } else {
       buffer.readBytes(ret)
       buffer.resetReaderIndex  // The buffer may be reread later
    }
    ret
  }
}
