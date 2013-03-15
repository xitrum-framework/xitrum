package xitrum.util

import java.nio.charset.Charset
import org.jboss.netty.buffer.ChannelBuffers
import org.jboss.netty.handler.codec.base64.{Base64 => B64, Base64Dialect}
import org.jboss.netty.util.CharsetUtil.UTF_8

/**
 * URL-safe dialect is used:
 * http://netty.io/3.6/api/org/jboss/netty/handler/codec/base64/Base64Dialect.html
 */
object UrlSafeBase64 {
  /**
   * The result contains no padding ("=" characters) so that it can be used as
   * request parameter name. (Netty POST body decoder prohibits "=" in parameter name.)
   *
   * See http://en.wikipedia.org/wiki/Base_64#Padding
   */
  def noPaddingEncode(bytes: Array[Byte]): String = {
    // No line break because the result may be used in HTTP response header (cookie)
    val buffer = B64.encode(ChannelBuffers.wrappedBuffer(bytes), false, Base64Dialect.URL_SAFE)
    val base64String = buffer.toString(UTF_8)
    removePadding(base64String)
  }

  /** @param base64String may contain padding ("=" characters) */
  def autoPaddingDecode(base64String: String): Option[Array[Byte]] = {
    try {
      val withPadding = addPadding(base64String)
      val buffer      = B64.decode(ChannelBuffers.copiedBuffer(withPadding, UTF_8), Base64Dialect.URL_SAFE)
      Some(ChannelBufferToBytes(buffer))
    } catch {
      case scala.util.control.NonFatal(e) => None
    }
  }

  // ---------------------------------------------------------------------------

  private def removePadding(base64String: String) = base64String.replace("=", "")

  private def addPadding(base64String: String) = {
    val mod = base64String.length % 4
    val padding = if (mod == 0) "" else if (mod == 1) "===" else if (mod == 2) "==" else if (mod == 3) "="
    base64String + padding
  }
}
