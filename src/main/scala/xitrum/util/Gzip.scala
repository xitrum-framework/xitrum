package xitrum.util

import java.io.ByteArrayOutputStream
import java.util.zip.GZIPOutputStream

object Gzip {
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
