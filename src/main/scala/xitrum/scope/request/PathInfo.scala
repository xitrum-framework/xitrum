package xitrum.scope.request

import java.net.URLDecoder
import xitrum.Config

/** URL: http://example.com/articles?page=2 => encoded: /articles */
class PathInfo(val encoded: String) {
  val tokens = {
    // http://stackoverflow.com/questions/785586/how-can-split-a-string-which-contains-only-delimiter
    // "/echo//".split("/")     => Array("", "echo")
    // "/echo//".split("/", -1) => Array("", "echo", "", "")
    //val encodeds = encoded.split("/", -1).filter(!_.isEmpty)
    val noSlashPrefix = if (encoded.startsWith("/")) encoded.substring(1) else encoded
    val encodeds      = noSlashPrefix.split("/", -1)

    encodeds.map(URLDecoder.decode(_, Config.xitrum.request.charsetName))
  }

  val decoded = "/" + tokens.mkString("/")

  val decodedWithIndexHtml =
    if (decoded.endsWith("/"))
      decoded + "index.html"
    else
      decoded + "/index.html"
}
