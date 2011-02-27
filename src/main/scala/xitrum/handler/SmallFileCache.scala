package xitrum.handler

import java.io.{File, RandomAccessFile}
import java.text.SimpleDateFormat
import scala.collection.mutable.HashMap

import xitrum.{Config, MimeType}

/** Cache is configureed by files_ehcache_name and files_max_size in xitrum.properties. */
object SmallFileCache {
  /** lastModified: See http://en.wikipedia.org/wiki/List_of_HTTP_header_fields */
  class GetResult
  case class  Hit(val bytes: Array[Byte], val lastModified: String, val mimeo: Option[String])                                  extends GetResult
  case object FileNotFound                                                                                                      extends GetResult
  case class  FileTooBig(val file: RandomAccessFile, val fileLength: Long, val lastModified: String, val mimeo: Option[String]) extends GetResult

  //                                                      lastModified  mime
  private val cache   = new HashMap[String, (Array[Byte], String,       Option[String])]
  private val rfc2822 = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z")

  def lastModified(timestamp: Long) = rfc2822.format(timestamp)

  /** abs: Absolute file path */
  def get(abs: String): GetResult = {
    val elem = cache.get(abs)
    if (elem != None) {
      val (bytes, lastModified, mimeo) = elem.get
      return Hit(bytes, lastModified, mimeo)
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

    val mimeo = MimeType.pathToMime(abs)

    val lm = lastModified(file.lastModified)

    // Cache if the file is small
    val fileLength = raf.length
    if (Config.isProductionMode && fileLength <= Config.cacheSmallStaticFileMaxSizeInKB * 1024) synchronized {
      val len = fileLength.toInt
      val bytes = new Array[Byte](len)

      // Read whole file
      var total = 0
      while (total < len) {
        val bytesRead = raf.read(bytes, total, len - total)
        total += bytesRead
      }

      raf.close
      cache(abs) = (bytes, lm, mimeo)
      return Hit(bytes, lm, mimeo)
    }

    return FileTooBig(raf, fileLength, lm, mimeo)
  }

  /**
   * You may use https://github.com/djpowell/liverepl to attach to a running
   * Xitrum process to clear the cache.
   */
  def clear {
    cache.clear
  }
}
