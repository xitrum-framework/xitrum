package xitrum.handler

import java.io.{File, RandomAccessFile}
import java.text.SimpleDateFormat
import scala.collection.mutable.HashMap

import xitrum.Config

/** Cache is configureed by files_ehcache_name and files_max_size in xitrum.properties. */
object SmallFileCache {
  /** lastModified: See http://en.wikipedia.org/wiki/List_of_HTTP_header_fields */
  class GetResult
  case class  Hit(val bytes: Array[Byte], val lastModified: String)                                  extends GetResult
  case object FileNotFound                                                                           extends GetResult
  case class  FileTooBig(val file: RandomAccessFile, val fileLength: Long, val lastModified: String) extends GetResult

  //                                                      lastModified
  private val cache   = new HashMap[String, (Array[Byte], String)]
  private val rfc2822 = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z")

  /** abs: Absolute file path */
  def get(abs: String): GetResult = {
    val elem = cache.get(abs)
    if (elem != None) {
      val (bytes, lastModified) = elem.get
      return Hit(bytes, lastModified)
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

    val lastModifiedL = file.lastModified
    val lastModified  = rfc2822.format(lastModifiedL)

    // Cache if the file is small
    val fileLength = raf.length
    if (Config.isProductionMode && fileLength <= Config.filesMaxSize) synchronized {
      val len = fileLength.toInt
      val bytes = new Array[Byte](len)

      // Read whole file
      var total = 0
      while (total < len) {
        val bytesRead = raf.read(bytes, total, len - total)
        total += bytesRead
      }

      raf.close
      cache(abs) = (bytes, lastModified)
      return Hit(bytes, lastModified)
    }

    return FileTooBig(raf, fileLength, lastModified)
  }

  /**
   * You may use https://github.com/djpowell/liverepl to attach to a running
   * Xitrum process to clear the cache.
   */
  def clear {
    cache.clear
  }
}
