package xitrum.util

import java.io.ByteArrayOutputStream
import java.util.zip.GZIPOutputStream

import org.jboss.netty.handler.codec.http.{HttpHeaders, HttpRequest}
import HttpHeaders.Names.ACCEPT_ENCODING

object Gzip {
  def isAccepted(request: HttpRequest) = {
    val acceptEncoding = request.getHeader(ACCEPT_ENCODING)
    (acceptEncoding != null && acceptEncoding.contains("gzip"))
  }

  def compress(bytes: Array[Byte]) = {
    val b = new ByteArrayOutputStream
    val g = new GZIPOutputStream(b)
    g.write(bytes)
    g.finish
    val gzippedBytes = b.toByteArray
    g.close
    gzippedBytes
  }
}
