package xitrum.util

import io.netty.buffer.{ByteBuf, CompositeByteBuf}

object ByteBufUtil {
  def toBytes(byteBuf: ByteBuf): Array[Byte] = {
    val len = byteBuf.readableBytes
    if (len == 0) return Array[Byte]()

    val ret = new Array[Byte](len)
    if (byteBuf.hasArray) {
      // https://github.com/netty/netty/issues/83
      System.arraycopy(byteBuf.array, 0, ret, 0, len)
    } else {
      // The buffer may be reread later
      byteBuf.markReaderIndex().readBytes(ret).resetReaderIndex()
    }
    ret
  }

  def writeComposite(compositeBuf: ByteBuf, component: ByteBuf) {
    // https://github.com/netty/netty/issues/2137
    val cb = compositeBuf.asInstanceOf[CompositeByteBuf]
    cb.addComponent(component).writerIndex(cb.writerIndex() + component.readableBytes)
  }
}
