package xt

import java.net.{URLDecoder => D}
import java.io.UnsupportedEncodingException

object URLDecoder {
  def decode(s: String): Option[String] = {
    try {
      val decoded = D.decode(s, "UTF-8")
      Some(decoded)
    } catch {
      case e: UnsupportedEncodingException =>
        try {
          val decoded = D.decode(s, "ISO-8859-1")
          Some(decoded)
        } catch {
          case _ => None
        }
    }
  }
}
