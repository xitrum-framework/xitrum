package xitrum.scope.request

import java.nio.charset.Charset

import io.netty.handler.codec.http.QueryStringDecoder

class PathInfo(decoder: QueryStringDecoder, charset: Charset) {
  // After being decoded, the original paths /test1/123%2F456 and /test1/123/456 will be the same
  val decoded: String = decoder.path

  val decodedWithIndexHtml: String = decoded + "/index.html"

  val encoded: String = {
    val uri  = decoder.uri
    val qPos = uri.indexOf("?")
    if (qPos >= 0) uri.substring(0, qPos) else uri
  }

  val tokens: Seq[String] = {
    // Need to split the original encoded URI (instead of decoder.path),
    // then decode the tokens (components),
    // otherwise /test1/123%2F456 will not match /test1/:p1
    val noSlashPrefix = if (encoded.startsWith("/")) encoded.substring(1) else encoded

    // http://stackoverflow.com/questions/785586/how-can-split-a-string-which-contains-only-delimiter
    // "/echo//".split("/")     => Array("", "echo")
    // "/echo//".split("/", -1) => Array("", "echo", "", "")
    noSlashPrefix.split("/", -1).map(t => QueryStringDecoder.decodeComponent(t, charset))
  }
}
