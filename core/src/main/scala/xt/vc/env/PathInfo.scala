package xt.vc.env

import xt.URLDecoder
import scala.collection.mutable.ArrayBuffer

class PathInfo(val encoded: String) {
  val decoded = URLDecoder.decode(encoded).get

  val tokens = {
    val encodeds = encoded.split("/").filter(_ != "")
    encodeds.map(URLDecoder.decode(_).get)
  }
}
