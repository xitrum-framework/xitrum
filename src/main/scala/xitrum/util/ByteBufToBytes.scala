package xitrum.util

import io.netty.buffer.ByteBuf

object ByteBufToBytes {
  def apply(byteBuf: ByteBuf): Array[Byte] = {
    val len = byteBuf.readableBytes
    val ret = new Array[Byte](len)
    if (byteBuf.hasArray) {
      // https://github.com/netty/netty/issues/83
      System.arraycopy(byteBuf.array, 0, ret, 0, len)
    } else {
       byteBuf.readBytes(ret)
       byteBuf.resetReaderIndex  // The buffer may be reread later
    }
    ret
  }
}
