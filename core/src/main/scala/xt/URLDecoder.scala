package xt

import java.net.{URLDecoder => D}
import java.io.UnsupportedEncodingException

object URLDecoder extends Logger {
  /** URL decode "value" using UTF-8 or ISO-8859-1 */
  def decode(s: String): Option[String] = {
    try {
      val decoded = D.decode(s, "UTF-8")
      Some(decoded)
    } catch {
      case e1: UnsupportedEncodingException =>
        logger.warn("URLDecoder failed with UTF-8", e1)

        try {
          val decoded = D.decode(s, "ISO-8859-1")
          Some(decoded)
        } catch {
          case e2 =>
            logger.warn("URLDecoder failed with ISO-8859-1", e2)
            None
        }
    }
  }
}
