package xitrum.etag

import java.io.File
import java.security.MessageDigest

import org.jboss.netty.buffer.ChannelBuffers
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

  //                                                     path    mtime
  private val smallFileCache        = new LocalLRUCache[(String, Long), Small](Config.config.response.maxCachedSmallStaticFiles)
  private val gzippedSmallFileCache = new LocalLRUCache[(String, Long), Small](Config.config.response.maxCachedSmallStaticFiles)

  def forBytes(bytes: Array[Byte]): String = {
    val md5 = MessageDigest.getInstance("MD5")  // MD5 is fastest
    md5.reset
    md5.update(bytes)
    Base64.encode(md5.digest)
  }

  def forString(string: String) = forBytes(string.getBytes(Config.config.request.charset))

  def forFile(path: String, gzipped: Boolean): Result = {
    val file = new File(path)
    if (!file.exists) return NotFound

    if (file.length > Config.config.response.smallStaticFileSizeInKB * 1024) return TooBig(file)

    val mtime = file.lastModified
    val key   = (path, mtime)
    val cache = if (gzipped) gzippedSmallFileCache else smallFileCache
    val value = cache.get(key)
    if (value != null) return value

    val bytes = Loader.bytesFromFile(path)
    if (bytes == null) return NotFound

    val etag    = forBytes(bytes)
    val small   = Small(bytes, etag, Mime.get(path), false)
    val smaller = if (gzipped) compressBigTextualFile(small) else small
    cache.synchronized { cache.put(key, smaller) }
    smaller
  }

  /**
   * Read whole file to memory. It's OK because the files are normally small.
   * No one is stupid enough to store large files in resources.
   */
  def forResource(path: String, gzipped: Boolean): Result = {
    val key   = ("[resource]" + path, 0l)
    val cache = if (gzipped) gzippedSmallFileCache else smallFileCache
    val value = cache.get(key)
    if (value != null) return value

    val bytes = Loader.bytesFromClasspath(path)
    if (bytes == null) return NotFound

    val etag    = forBytes(bytes)
    val small   = Small(bytes, etag, Mime.get(path), false)
    val smaller = if (gzipped) compressBigTextualFile(small) else small
    cache.synchronized { cache.put(key, smaller) }
    smaller
  }

  //----------------------------------------------------------------------------

  def areEtagsIdentical(request: HttpRequest, etag: String) =
    (request.getHeader(IF_NONE_MATCH) == etag)

  /** @return true if NOT_MODIFIED response has been sent */
  def respondIfEtagsIdentical(action: Action, etag: String) = {
    val request  = action.request
    val response = action.response
    if (areEtagsIdentical(request, etag)) {
      response.setStatus(NOT_MODIFIED)
      HttpHeaders.setContentLength(response, 0)
      response.setContent(ChannelBuffers.EMPTY_BUFFER)
      action.respond
      true
    } else {
      request.setHeader(ETAG, etag)
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
