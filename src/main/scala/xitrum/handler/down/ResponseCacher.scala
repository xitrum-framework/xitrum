package xitrum.handler.down

import java.io.{ByteArrayOutputStream, Serializable}
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPOutputStream
import scala.collection.immutable.TreeMap

import org.jboss.netty.channel.{ChannelHandler, SimpleChannelDownstreamHandler, ChannelHandlerContext, MessageEvent, Channels, ChannelFutureListener}
import org.jboss.netty.buffer.ChannelBuffers
import ChannelHandler.Sharable
import org.jboss.netty.handler.codec.http.{DefaultHttpResponse, HttpHeaders, HttpResponse, HttpResponseStatus, HttpVersion}
import HttpHeaders.Names.{CONTENT_ENCODING, CONTENT_TYPE}

import xitrum.Cache
import xitrum.action.Action
import xitrum.action.env.{Env => AEnv}
import xitrum.action.routing.Routes
import xitrum.handler.Env

object ResponseCacher {
  private val GZIP_THRESHOLD_KB = 10

  def makeCacheKey(action: Action): String = {
    val params    = action.textParams
    val sortedMap = (new TreeMap[String, List[String]]) ++ params
    "page/action cache/" + action.getClass.getName + "/" + sortedMap.toString
  }

    /**
   * Response can be (re)constructed from (status, headers, content, compressed).
   * To be stored in cache, these must be Serializable. We choose:
   *   status:  Int
   *   headers: Array[(String, String)]
   *   content: Array[Byte]
   *   gzipped: Boolean  // Big textual content is gzipped to save memory
   */
  def serializeResponse(response: HttpResponse): Serializable = {
    val status  = response.getStatus.getCode
    val headers = {
      val list = response.getHeaders  // JList[JMap.Entry[String, String]], JMap.Entry is not Serializable!
      val size = list.size
      val ret  = new Array[(String, String)](size)
      for (i <- 0 until size) {
        val m = list.get(i)
        ret(i) = (m.getKey, m.getValue)
      }
      ret
    }
    val (bytes, gzipped) = tryGZIPBigTextualContent(response)
    (status, headers, bytes, gzipped).asInstanceOf[Serializable]
  }

  /** This is the reverse of serializeResponse. */
  def deserializeToResponse(serializable: Serializable): HttpResponse = {
    val (status, headers, bytes, gzipped) = serializable.asInstanceOf[(Int, Array[(String, String)], Array[Byte], Boolean)]

    val response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.valueOf(status))
    for ((k, v) <- headers) response.addHeader(k, v)
    response.setContent(ChannelBuffers.wrappedBuffer(bytes))
    if (gzipped) response.setHeader(CONTENT_ENCODING , "gzip")

    response
  }

  //----------------------------------------------------------------------------

  private def tryGZIPBigTextualContent(response: HttpResponse): (Array[Byte], Boolean) = {
    val bytes   = {
      val channelBuffer = response.getContent
      val ret = new Array[Byte](channelBuffer.readableBytes)
      channelBuffer.readBytes(ret)
      channelBuffer.resetReaderIndex
      ret
    }

    val contentType = response.getHeader(CONTENT_TYPE)

    // No content type, don't know if this the response is textual
    if (contentType == null) return (bytes, false)

    val lower   = contentType.toLowerCase();
    val textual = (lower.indexOf("text") >= 0 || lower.indexOf("xml") >= 0 || lower.indexOf("javascript") >= 0 || lower.indexOf("json") >= 0)
    if (!textual) return (bytes, false)

    val big = bytes.length > GZIP_THRESHOLD_KB * 1024
    if (!big) return (bytes, false)

    // Compress
    val b = new ByteArrayOutputStream
    val g = new GZIPOutputStream(b)
    g.write(bytes)
    g.finish
    val compressedBytes = b.toByteArray
    g.close

    (compressedBytes, true)
  }
}

@Sharable
class ResponseCacher extends SimpleChannelDownstreamHandler {
  import ResponseCacher._

  override def writeRequested(ctx: ChannelHandlerContext, e: MessageEvent) {
    val m = e.getMessage
    if (!m.isInstanceOf[Env]) {
      ctx.sendDownstream(e)
      return
    }

    val env      = m.asInstanceOf[Env]
    val action   = env.action

    // action may be null when the request could not go to Dispatcher, for
    // example when the response is served from PublicResourceServer
    if (action == null) {
      ctx.sendDownstream(e)
      return
    }

    val actionClass = action.getClass.asInstanceOf[Class[Action]]
    val cacheSecs   = Routes.getCacheSecs(actionClass)

    if (cacheSecs != 0) {
      val key = makeCacheKey(action)
      if (!Cache.cache.containsKey(key)) {  // Check to avoid the cost of serializing
        val response     = env.response
        val serializable = serializeResponse(response)
        val secs         = if (cacheSecs < 0) -cacheSecs else cacheSecs
        Cache.cache.putIfAbsent(key, serializable, secs, TimeUnit.SECONDS)
      }
    }

    ctx.sendDownstream(e)
  }
}
