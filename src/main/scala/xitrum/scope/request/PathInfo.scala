package xitrum.scope.request

/** URL: http://example.com/articles?page=2 => encoded: /articles */
class PathInfo(val decoded: String) {
  val tokens = {
    // http://stackoverflow.com/questions/785586/how-can-split-a-string-which-contains-only-delimiter
    // "/echo//".split("/")     => Array("", "echo")
    // "/echo//".split("/", -1) => Array("", "echo", "", "")
    //val encodeds = encoded.split("/", -1).filter(!_.isEmpty)
    val noSlashPrefix = if (decoded.startsWith("/")) decoded.substring(1) else decoded
    noSlashPrefix.split("/", -1)
  }

  val decodedWithIndexHtml = decoded + "/index.html"
}
