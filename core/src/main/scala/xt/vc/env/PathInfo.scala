package xt.vc.env

import xt.URLDecoder
import scala.collection.mutable.ArrayBuffer

/** URL: http://example.com/articles?page=2 => pathInfo: /articles */
class PathInfo(val encoded: String) {
  val decoded = URLDecoder.decode(encoded).get

  val tokens = {
    val encodeds = encoded.split("/").filter(_ != "")
    encodeds.map(URLDecoder.decode(_).get)
  }
}
