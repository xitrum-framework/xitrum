package xt.vc.env.session

import java.nio.charset.Charset
import org.jboss.netty.buffer.ChannelBuffers
import org.jboss.netty.handler.codec.base64.{Base64 => B64, Base64Dialect}

object Base64 {
  def decode(base64String: String): Option[Array[Byte]] = {
    try {
      val buffer = B64.decode(ChannelBuffers.copiedBuffer(base64String, Charset.forName("UTF-8")))
      val bytes  = new Array[Byte](buffer.readableBytes)
      buffer.readBytes(bytes)
      Some(bytes)
    } catch {
      case _ => None
    }
  }

  def encode(bytes: Array[Byte]): String = {
    // No line break because the result may be used in HTTP response header (cookie)
    val buffer = B64.encode(ChannelBuffers.copiedBuffer(bytes), false)
    buffer.toString(Charset.forName("UTF-8"))
  }
}
