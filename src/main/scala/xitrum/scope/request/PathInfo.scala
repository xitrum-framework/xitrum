package xitrum.scope.request

import java.net.URLDecoder
import scala.collection.mutable.ArrayBuffer

import xitrum.Config

/** URL: http://example.com/articles?page=2 => pathInfo: /articles */
class PathInfo(val encoded: String) {
  val tokens = {
    val encodeds = encoded.split('/').filter(_ != "")
    encodeds.map(URLDecoder.decode(_, Config.config.request.charset))
  }

  val decoded = "/" + tokens.mkString("/")
}
