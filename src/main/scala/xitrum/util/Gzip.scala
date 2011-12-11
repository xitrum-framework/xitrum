package xitrum.util

import java.io.ByteArrayOutputStream
import java.util.zip.GZIPOutputStream

import io.netty.handler.codec.http.{HttpHeaders, HttpRequest, HttpResponse}
import HttpHeaders.Names.{ACCEPT_ENCODING, CONTENT_ENCODING, CONTENT_TYPE}
import io.netty.buffer.ChannelBuffers

import xitrum.Config

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

  /**
   * If compressed, CONTENT_LENGTH is updated and CONTENT_ENCODING is set to "gzip".
   *
   * @return Response body content as bytes
   */
  def tryCompressBigTextualResponse(request: HttpRequest, response: HttpResponse): Array[Byte] = {
    val channelBuffer = response.getContent
    val bytes         = ChannelBufferToBytes(channelBuffer)

    if (!isAccepted(request) ||
        response.containsHeader(CONTENT_ENCODING) ||
        !Mime.isTextual(response.getHeader(CONTENT_TYPE)) ||
        bytes.length < Config.BIG_TEXTUAL_RESPONSE_SIZE_IN_KB * 1024) {
      return bytes
    }

    val gzippedBytes = compress(bytes)

    // Update CONTENT_LENGTH and set CONTENT_ENCODING
    HttpHeaders.setContentLength(response, gzippedBytes.length)
    response.setHeader(CONTENT_ENCODING, "gzip")

    response.setContent(ChannelBuffers.wrappedBuffer(gzippedBytes))
    gzippedBytes
  }
}
