package xt.vc.env.session

import java.nio.charset.Charset
import org.jboss.netty.buffer.ChannelBuffers
import org.jboss.netty.handler.codec.base64.{Base64 => B64, Base64Dialect}

object Base64 {
  def decode(base64String: String): Array[Byte] = {
    val buffer = B64.decode(ChannelBuffers.copiedBuffer(base64String, Charset.forName("UTF-8")), Base64Dialect.URL_SAFE)
    val bytes  = new Array[Byte](buffer.readableBytes)
    buffer.readBytes(bytes)
    bytes
  }

  def encode(bytes: Array[Byte]): String = {
    val buffer = B64.encode(ChannelBuffers.copiedBuffer(bytes), Base64Dialect.URL_SAFE)
    buffer.toString(Charset.forName("UTF-8"))
  }
}
