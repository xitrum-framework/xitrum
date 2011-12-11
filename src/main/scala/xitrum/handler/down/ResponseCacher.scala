package xitrum.handler.down

import scala.collection.immutable.{SortedMap, TreeMap}

import io.netty.channel.{ChannelHandler, SimpleChannelDownstreamHandler, ChannelHandlerContext, MessageEvent, Channels, ChannelFutureListener}
import io.netty.buffer.ChannelBuffers
import ChannelHandler.Sharable
import io.netty.handler.codec.http.{DefaultHttpResponse, HttpHeaders, HttpRequest, HttpResponse, HttpResponseStatus, HttpVersion}
import HttpResponseStatus.OK
import HttpHeaders.Names.{CONTENT_ENCODING, CONTENT_TYPE}

import xitrum.{Cache, Config, Logger}
import xitrum.Action
import xitrum.routing.Routes
import xitrum.scope.request.RequestEnv
import xitrum.handler.HandlerEnv
import xitrum.util.{Gzip, Mime}

object ResponseCacher extends Logger {
  //                             statusCode  headers                  content
  private type CachedResponse = (Int,        Array[(String, String)], Array[Byte])

  def cacheResponse(action: Action) {
    val actionClass = action.getClass.asInstanceOf[Class[Action]]
    val cacheSecs   = Routes.getCacheSecs(actionClass)
    if (cacheSecs == 0) return

    val key = makeCacheKey(action)
    if (!Cache.cache.containsKey(key)) {  // Check to avoid the cost of serializing
      val response       = action.response
      val cachedResponse = serializeResponse(action.request, response)
      val secs           = if (cacheSecs < 0) -cacheSecs else cacheSecs
      Cache.putIfAbsentSecond(key, cachedResponse, secs)
    }
  }

  def getCachedResponse(action: Action): Option[HttpResponse] = {
    val key = makeCacheKey(action)
    Cache.getAs[CachedResponse](key).map(deserializeToResponse)
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
  private def serializeResponse(request: HttpRequest, response: HttpResponse): CachedResponse = {
    val status = response.getStatus.getCode

    // Should be before extracting headers, because the CONTENT_LENGTH header
    // can be updated if the content if gzipped
    val bytes = Gzip.tryCompressBigTextualResponse(request, response)

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

    (status, headers, bytes)
  }

  private def deserializeToResponse(cachedResponse: CachedResponse): HttpResponse = {
    val (status, headers, bytes) = cachedResponse
    val response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.valueOf(status))
    for ((k, v) <- headers) response.addHeader(k, v)
    response.setContent(ChannelBuffers.wrappedBuffer(bytes))
    response
  }

  // uploadParams is not included in the key, only textParams is
  private def makeCacheKey(action: Action): String = {
    // See xitrum.action.env.Env.Params
    val sortedMap = (new TreeMap[String, List[String]]) ++ action.textParams

    val request = action.request
    val key     =
      Cache.pageActionPrefix(action.getClass.asInstanceOf[Class[Action]]) + "/" +
      request.getMethod + "/" +
      inspectSortedParams(sortedMap)
    if (Gzip.isAccepted(request)) key + "_gzipped" else key
  }

  // See RequestEnv.inspectParamsWithFilter
  private def inspectSortedParams(params: SortedMap[String, List[String]]) = {
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
}

@Sharable
class ResponseCacher extends SimpleChannelDownstreamHandler with Logger {
  import ResponseCacher._

  override def writeRequested(ctx: ChannelHandlerContext, e: MessageEvent) {
    val m = e.getMessage
    if (!m.isInstanceOf[HandlerEnv]) {
      ctx.sendDownstream(e)
      return
    }

    val env    = m.asInstanceOf[HandlerEnv]
    val action = env.action

    // action may be null when the request could not go to Dispatcher, for
    // example when the response is served from PublicResourceServer
    if (action == null) {
      ctx.sendDownstream(e)
      return
    }

    val response = action.response
    if (response.getStatus == OK && !response.isChunked) cacheResponse(action)

    ctx.sendDownstream(e)
  }
}
