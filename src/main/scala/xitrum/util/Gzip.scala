package xitrum.util

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.util.zip.{GZIPInputStream, GZIPOutputStream}

import io.netty.buffer.{CompositeByteBuf, Unpooled}
import io.netty.handler.codec.http.{HttpHeaders, HttpRequest, FullHttpResponse}
import HttpHeaders.Names.{ACCEPT_ENCODING, CONTENT_ENCODING, CONTENT_TYPE}

import xitrum.Config

object Gzip {
  // http://stackoverflow.com/questions/4818468/how-to-check-if-inputstream-is-gzipped
  private val GZIP_SIGNATURE_BYTE1 = GZIPInputStream.GZIP_MAGIC.toByte
  private val GZIP_SIGNATURE_BYTE2 = (GZIPInputStream.GZIP_MAGIC >> 8).toByte

  def isCompressed(bytes: Array[Byte]) = {
    if (bytes.length < 2)
      false
    else
      bytes(0) == GZIP_SIGNATURE_BYTE1 && bytes(1) == GZIP_SIGNATURE_BYTE2
  }

  def compress(bytes: Array[Byte]) = {
    val b = new ByteArrayOutputStream
    val g = new GZIPOutputStream(b)
    g.write(bytes)
    g.finish()
    val ret = b.toByteArray
    g.close()
    b.close()
    ret
  }

  def uncompress(bytes: Array[Byte]) = {
    val b = new ByteArrayInputStream(bytes)
    val g = new GZIPInputStream(b)

    val acc = new ByteArrayOutputStream
    val tmp = new Array[Byte](1024)
    var len = 0
    do {
      len = g.read(tmp)
      if (len > 0) acc.write(tmp, 0, len)
    } while (len > 0)
    val ret = acc.toByteArray
    acc.close()

    g.close()
    b.close()
    ret
  }

  /** @return The uncompressed bytes, or the input itself if it's not compressed */
  def mayUncompress(maybeCompressed: Array[Byte]) =
    if (isCompressed(maybeCompressed)) uncompress(maybeCompressed) else maybeCompressed

  def isAccepted(request: HttpRequest) = {
    if (Config.xitrum.response.autoGzip) {
      val acceptEncoding = HttpHeaders.getHeader(request, ACCEPT_ENCODING)
      acceptEncoding != null && acceptEncoding.contains("gzip")
    } else {
      false
    }
  }

  /**
   * If compressed, CONTENT_LENGTH is updated and CONTENT_ENCODING is set to "gzip".
   *
   * @return Response body content as bytes
   */
  def tryCompressBigTextualResponse(
      gzipAccepted: Boolean,
      response:  FullHttpResponse,
      needBytes: Boolean
  ): Array[Byte] =
  {
    // See Request2Env
    val cb = response.content.asInstanceOf[CompositeByteBuf]

    if (!gzipAccepted ||
        response.headers.contains(CONTENT_ENCODING) ||
        !Mime.isTextual(HttpHeaders.getHeader(response, CONTENT_TYPE)) ||
        cb.readableBytes < Config.BIG_TEXTUAL_RESPONSE_SIZE_IN_KB * 1024
    ) {
      return if (needBytes) ByteBufUtil.toBytes(cb) else null
    }

    val bytes        = ByteBufUtil.toBytes(cb)
    val gzippedBytes = compress(bytes)

    // Update CONTENT_LENGTH and set CONTENT_ENCODING
    HttpHeaders.setContentLength(response, gzippedBytes.length)
    HttpHeaders.setHeader(response, CONTENT_ENCODING, "gzip")

    cb.removeComponents(0, cb.numComponents)
    cb.clear()
    ByteBufUtil.writeComposite(cb, Unpooled.wrappedBuffer(gzippedBytes))
    if (needBytes) gzippedBytes else null
  }
}
