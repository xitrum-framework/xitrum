package xt.vc.env

import java.net.URLDecoder
import scala.collection.mutable.ArrayBuffer

/** URL: http://example.com/articles?page=2 => pathInfo: /articles */
class PathInfo(val encoded: String) {
  val decoded = URLDecoder.decode(encoded, "UTF-8")

  val tokens = {
    val encodeds = encoded.split("/").filter(_ != "")
    encodeds.map(URLDecoder.decode(_, "UTF-8"))
  }
}
