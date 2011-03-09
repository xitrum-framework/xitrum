package xitrum.handler

import java.io.{File, RandomAccessFile}
import scala.collection.mutable.HashMap

import xitrum.{Cache, Config, Gzip, Mime}

object SmallFileCache {
  /** lastModified: See http://en.wikipedia.org/wiki/List_of_HTTP_header_fields */
  class GetResult
  case class  Hit(val bytes: Array[Byte], val gzipped: Boolean, val lastModified: String, val mimeo: Option[String])            extends GetResult
  case object FileNotFound                                                                                                      extends GetResult
  case class  FileTooBig(val file: RandomAccessFile, val fileLength: Long, val lastModified: String, val mimeo: Option[String]) extends GetResult

  //                         body         gzipped  lastModified         MIME
  private type CachedFile = (Array[Byte], Boolean, String,       Option[String])

  /**
   * If abs points to a hidden file, this method returns FileNotFound.
   *
   * @param abs Absolute file path
   */
  def get(abs: String): GetResult = {
    Cache.getAs[CachedFile](abs) match {
      case None =>

      case Some(cachedFile) =>
        val (bytes, gzipped, lastModified, mimeo) = cachedFile
        return Hit(bytes, gzipped, lastModified, mimeo)
    }

    // For security do not return hidden file
    val file = new File(abs)
    if (!file.exists || file.isHidden) return FileNotFound

    var raf: RandomAccessFile = null
    try {
      raf = new RandomAccessFile(abs, "r")
    } catch {
      case _ => return FileNotFound
    }

    val lm    = NotModified.formatRfc2822(file.lastModified)
    val mimeo = Mime.get(abs)

    // Cache if the file is small
    val fileLength = raf.length
    if (Config.isProductionMode && fileLength <= Config.cacheSmallStaticFileMaxSizeInKB * 1024) synchronized {
      // Read whole file
      val len   = fileLength.toInt
      val bytes = new Array[Byte](len)
      var total = 0
      while (total < len) {
        val bytesRead = raf.read(bytes, total, len - total)
        total += bytesRead
      }
      raf.close

      val (bytes2, gzipped) =
        if (fileLength > Config.compressBigTextualResponseMinSizeInKB * 1024 && mimeo.isDefined && Mime.isTextual(mimeo.get)) {
          (Gzip.compress(bytes), true)
        } else {
          (bytes, false)
        }
      val cachedFile = (bytes2, gzipped, lm, mimeo)

      Cache.putIfAbsentMinute(abs, cachedFile, NotModified.TTL_IN_MINUTES)
      return Hit(bytes2, gzipped, lm, mimeo)
    }

    return FileTooBig(raf, fileLength, lm, mimeo)
  }
}
