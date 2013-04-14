package xitrum.handler.down

import scala.collection.immutable.{SortedMap, TreeMap}

import org.jboss.netty.channel.{ChannelHandler, SimpleChannelDownstreamHandler, ChannelHandlerContext, MessageEvent, Channels}
import org.jboss.netty.buffer.ChannelBuffers
import ChannelHandler.Sharable
import org.jboss.netty.handler.codec.http.{DefaultHttpResponse, HttpHeaders, HttpRequest, HttpResponse, HttpResponseStatus, HttpVersion}
import HttpResponseStatus.OK
import HttpHeaders.Names.{CONTENT_ENCODING, CONTENT_TYPE}

import xitrum.{Cache, Config, Logger}
import xitrum.Action
import xitrum.scope.request.RequestEnv
import xitrum.handler.HandlerEnv
import xitrum.util.{Gzip, Mime}

object ResponseCacher extends Logger {
  //                             statusCode  headers           content
  private type CachedResponse = (Int, Array[(String, String)], Array[Byte])

  def cacheResponse(env: HandlerEnv) {
    val cacheSecs = env.route.cacheSecs
    val key       = makeCacheKey(env)
    if (!Cache.cache.containsKey(key)) { // Check to avoid the cost of serializing
      val response          = env.response
      val cachedResponse    = serializeResponse(env.request, response)
      val positiveCacheSecs = if (cacheSecs < 0) -cacheSecs else cacheSecs
      Cache.putIfAbsentSecond(key, cachedResponse, positiveCacheSecs)
    }
  }

  def getCachedResponse(env: HandlerEnv): Option[HttpResponse] = {
    val key = makeCacheKey(env)
    Cache.getAs[CachedResponse](key).map(deserializeToResponse)
  }

  //----------------------------------------------------------------------------

  /**
   * Response can be (re)constructed from (status, headers, content, compressed).
   * To be stored in cache, these must be Serializable. We choose:
   *   status:  Int
   *   headers: Array[(String, String)]
   *   content: Array[Byte]
   *   gzipped: Boolean, big textual content is gzipped to save memory
   */
  private def serializeResponse(request: HttpRequest, response: HttpResponse): CachedResponse = {
    val status = response.getStatus.getCode

    // Should be before extracting headers, because the CONTENT_LENGTH header
    // can be updated if the content if gzipped
    val bytes = Gzip.tryCompressBigTextualResponse(request, response)

    val headers = {
      val list = response.getHeaders // JList[JMap.Entry[String, String]], JMap.Entry is not Serializable!
      val size = list.size
      val ret = new Array[(String, String)](size)
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
  private def makeCacheKey(env: HandlerEnv): String = {
    // See xitrum.scope.request.Params in xitrum/scope/request/package.scala
    val sortedMap = (new TreeMap[String, Seq[String]]) ++ env.textParams

    val request = env.request
    val key =
      Cache.pageActionPrefix(env.route.klass.asInstanceOf[Class[Action]]) + "/" +
      request.getMethod + "/" +
      inspectSortedParams(sortedMap)
    if (Gzip.isAccepted(request)) key + "_gzipped" else key
  }

  // See RequestEnv.inspectParamsWithFilter
  private def inspectSortedParams(params: SortedMap[String, Seq[String]]) = {
    val sb = new StringBuilder
    sb.append("{")

    val keys = params.keys.toArray
    val size = keys.size
    for (i <- 0 until size) {
      val key = keys(i)
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

    val env   = m.asInstanceOf[HandlerEnv]
    val route = env.route

    // action may be null when the request could not go to Dispatcher, for
    // example when the response is served from PublicResourceServer
    if (route == null) {
      ctx.sendDownstream(e)
      return
    }

    val response = env.response
    if (response.getStatus == OK && !response.isChunked && env.route.cacheSecs != 0) cacheResponse(env)

    ctx.sendDownstream(e)
  }
}
