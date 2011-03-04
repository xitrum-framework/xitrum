package xitrum.handler.down

import java.io.{ByteArrayOutputStream, Serializable}
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPOutputStream
import scala.collection.immutable.{SortedMap, TreeMap}

import org.jboss.netty.channel.{ChannelHandler, SimpleChannelDownstreamHandler, ChannelHandlerContext, MessageEvent, Channels, ChannelFutureListener}
import org.jboss.netty.buffer.ChannelBuffers
import ChannelHandler.Sharable
import org.jboss.netty.handler.codec.http.{DefaultHttpResponse, HttpHeaders, HttpResponse, HttpResponseStatus, HttpVersion}
import HttpHeaders.Names.{CONTENT_ENCODING, CONTENT_TYPE}

import xitrum.{Config, Cache, Logger}
import xitrum.action.Action
import xitrum.action.env.{Env => AEnv}
import xitrum.action.routing.Routes
import xitrum.handler.Env

object ResponseCacher extends Logger {
  private val GZIP_THRESHOLD_KB = 10

  def cacheResponse(action: Action) {
    val actionClass = action.getClass.asInstanceOf[Class[Action]]
    val cacheSecs   = Routes.getCacheSecs(actionClass)
    if (cacheSecs == 0) return

    val key = makeCacheKey(action)

    // FIXME
    // Sometimes Hazelcast 1.9.2.1 has problem with calling containsKey:
    //   java.lang.RuntimeException: java.lang.reflect.InvocationTargetException
    //     at com.hazelcast.impl.FactoryImpl$MProxyImpl$DynamicInvoker.invoke(FactoryImpl.java:1900) ~[hazelcast-1.9.2.1.jar:na]
    //     at $Proxy0.containsKey(Unknown Source) ~[na:na]
    //     at com.hazelcast.impl.FactoryImpl$MProxyImpl.containsKey(FactoryImpl.java:2100) ~[hazelcast-1.9.2.1.jar:na]
    val cached = try {
      Cache.cache.containsKey(key)
    } catch {
      case e =>
        logger.warn("containsKey error", e)
        false
    }

    if (!cached) {  // Check to avoid the cost of serializing
      val response     = action.response
      val serializable = serializeResponse(response)
      val secs         = {  // See Config.non200ResponseCacheTTLInSecs
        val cs = if (cacheSecs < 0) -cacheSecs else cacheSecs
        if (response.getStatus == HttpResponseStatus.OK) {
          cs
        } else {
          val ret = if (cs < Config.non200ResponseCacheTTLInSecs) cs else Config.non200ResponseCacheTTLInSecs
          logger.info("Cache non 200 response for {} secs, key: {}", ret, key)
          ret
        }
      }

      Cache.cache.putIfAbsent(key, serializable, secs, TimeUnit.SECONDS)
    }
  }

  def getCachedResponse(action: Action): Option[HttpResponse] = {
    val key   = ResponseCacher.makeCacheKey(action)
    val value = Cache.cache.get(key)
    if (value == null) {
      None
    } else {
      // Application version up etc. may cause cache restoring to be failed.
      // In this case, we remove the cache.
      deserializeToResponse(value.asInstanceOf[Serializable]) match {
        case None =>
          logger.warn("Response cache restore failed, will now remove it, key: {}", key)
          Cache.cache.remove(key)
          None

        case someResponse =>
          someResponse
      }
    }
  }

  //----------------------------------------------------------------------------

  /**
   * Response can be (re)constructed from (status, headers, content, compressed).
   * To be stored in cache, these must be Serializable. We choose:
   *   status:  Int
   *   headers: Array[(String, String)]
   *   content: Array[Byte]
   *   gzipped: Boolean  // Big textual content is gzipped to save memory
   */
  private def serializeResponse(response: HttpResponse): Serializable = {
    val status = response.getStatus.getCode

    // Should be before extracting headers, because the CONTENT_LENGTH header
    // can be updated if the content if gzipped
    val bytes = gzipBigTextualContent(response)

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

    (status, headers, bytes).asInstanceOf[Serializable]
  }

  private def deserializeToResponse(serializable: Serializable): Option[HttpResponse] = {
    try {
      val (status, headers, bytes) = serializable.asInstanceOf[(Int, Array[(String, String)], Array[Byte])]
      val response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.valueOf(status))
      for ((k, v) <- headers) response.addHeader(k, v)
      response.setContent(ChannelBuffers.wrappedBuffer(bytes))

      Some(response)
    } catch {
      case _ => None
    }
  }

  // uploadParams is not included in the key, only textParams is
  private def makeCacheKey(action: Action): String = {
    val textParams = action.textParams
    val sortedMap =
      (new TreeMap[String, Array[String]]) ++  // See xitrum.action.env.Env.Params
      textParams
    "xitrum/page-action/" + action.request.getMethod + "/" + action.getClass.getName + "/" + inspectSortedParams(sortedMap)
  }

  // See xitrum.action.env.Env.inspectParams
  private def inspectSortedParams(params: SortedMap[String, Array[String]]) = {
    val sb = new StringBuilder
    sb.append("{")

    val keys = params.keys.toArray
    val size = keys.size
    for (i <- 0 until size) {
      val key    = keys(i)
      val values = params(key)

      sb.append(key)
      sb.append(": ")

      if (values.length == 0) {
        sb.append("[EMPTY]")
      } else if (values.length == 1) {
        sb.append(values(0))
      } else {
        sb.append("[")
        sb.append(values.mkString(", "))
        sb.append("]")
      }

      if (i < size - 1) sb.append(", ")
    }

    sb.append("}")
    sb.toString
  }

  // If gzipped, CONTENT_LENGTH header is updated and CONTENT_ENCODING is set to gzip.
  private def gzipBigTextualContent(response: HttpResponse): Array[Byte] = {
    val bytes = {
      val channelBuffer = response.getContent
      val ret           = new Array[Byte](channelBuffer.readableBytes)
      channelBuffer.readBytes(ret)
      channelBuffer.resetReaderIndex
      ret
    }

    if (!isTextualResponse(response)) return bytes

    val big = bytes.length > GZIP_THRESHOLD_KB * 1024
    if (!big) return bytes

    // Compress
    val b = new ByteArrayOutputStream
    val g = new GZIPOutputStream(b)
    g.write(bytes)
    g.finish
    val compressedBytes = b.toByteArray
    g.close

    // Update CONTENT_LENGTH and set CONTENT_ENCODING
    HttpHeaders.setContentLength(response, compressedBytes.length)
    response.setHeader(CONTENT_ENCODING, "gzip")

    response.setContent(ChannelBuffers.wrappedBuffer(compressedBytes))
    compressedBytes
  }

  private def isTextualResponse(response: HttpResponse) = {
    val contentType = response.getHeader(CONTENT_TYPE)
    if (contentType == null) {
      false
    } else {
      val lower = contentType.toLowerCase
      (lower.indexOf("text") >= 0 || lower.indexOf("xml") >= 0 || lower.indexOf("javascript") >= 0 || lower.indexOf("json") >= 0)
    }
  }
}

@Sharable
class ResponseCacher extends SimpleChannelDownstreamHandler with Logger {
  import ResponseCacher._

  override def writeRequested(ctx: ChannelHandlerContext, e: MessageEvent) {
    val m = e.getMessage
    if (!m.isInstanceOf[Env]) {
      ctx.sendDownstream(e)
      return
    }

    val env    = m.asInstanceOf[Env]
    val action = env.action

    // action may be null when the request could not go to Dispatcher, for
    // example when the response is served from PublicResourceServer
    if (action == null) {
      ctx.sendDownstream(e)
      return
    }

    cacheResponse(action)
    ctx.sendDownstream(e)
  }
}
