package xitrum.etag

import java.io.File
import java.security.MessageDigest

import org.jboss.netty.handler.codec.http.{HttpHeaders, HttpRequest, HttpResponse, HttpResponseStatus}
import HttpHeaders.Names._
import HttpHeaders.Values._
import HttpResponseStatus._

import xitrum.{Action, Config, Logger}
import xitrum.util.{Base64, Gzip, Loader, LocalLRUCache, Mime}

object Etag extends Logger {
  class Result
  case object NotFound                                                                                         extends Result
  case class  TooBig(file: File)                                                                               extends Result
  case class  Small(val bytes: Array[Byte], val etag: String, val mimeo: Option[String], val gzipped: Boolean) extends Result

  //                                              path    mtime
  private val smallFileCache = new LocalLRUCache[(String, Long), Small](Config.maxCachedSmallStaticFiles)

  def forBytes(bytes: Array[Byte]): String = {
    val md5 = MessageDigest.getInstance("MD5")  // MD5 is fastest
    md5.reset
    md5.update(bytes)
    Base64.encode(md5.digest)
  }

  def forString(string: String) = forBytes(string.getBytes(Config.paramCharsetName))

  def forFile(path: String): Result = {
    val file = new File(path)
    if (!file.exists) {
      logger.warn("File not found: " + file.getAbsolutePath)
      return NotFound
    }

    if (file.length > Config.smallStaticFileSizeInKB * 1024) return TooBig(file)

    val mtime = file.lastModified
    val key   = (path, mtime)
    val value = smallFileCache.get(key)
    if (value != null) return value

    val bytes = Loader.bytesFromFile(path)
    if (bytes == null) return NotFound

    val etag    = forBytes(bytes)
    val small   = Small(bytes, etag, Mime.get(path), false)
    val smaller = compressBigTextualFile(small)
    smallFileCache.synchronized { smallFileCache.put(key, smaller) }
    smaller
  }

  /**
   * Read whole file to memory. It's OK because the files are normally small.
   * No one is stupid enough to store large files in resources.
   */
  def forResource(path: String): Result = {
    val bytes = Loader.bytesFromClasspath(path)
    if (bytes == null) {
      logger.warn("Resource not found: " + path)
      return NotFound
    }

    val key   = ("[resource]" + path, 0l)
    val value = smallFileCache.get(key)
    if (value != null) return value

    val etag    = forBytes(bytes)
    val small   = Small(bytes, etag, Mime.get(path), false)
    val smaller = compressBigTextualFile(small)
    smallFileCache.synchronized { smallFileCache.put(key, smaller) }
    smaller
  }

  //----------------------------------------------------------------------------

  /** @return true if NOT_MODIFIED response has been sent */
  def respondIfEtagsMatch(action: Action, etag: String) = {
    if (action.request.getHeader(IF_NONE_MATCH) == etag) {
      action.response.setStatus(NOT_MODIFIED)
      action.respond
      true
    } else {
      action.request.setHeader(ETAG, etag)
      false
    }
  }

  //----------------------------------------------------------------------------

  // Always compress all text file because these are static file,
  // the compression is only done once
  private def compressBigTextualFile(small: Small): Small = {
    val (bytes2, gzipped) =
      if (small.mimeo.isDefined && Mime.isTextual(small.mimeo.get))
        (Gzip.compress(small.bytes), true)
      else
        (small.bytes, false)

    Small(bytes2, small.etag, small.mimeo, gzipped)
  }
}
