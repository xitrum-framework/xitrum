package xitrum.handler.down

import scala.collection.immutable.{ SortedMap, TreeMap }

import io.netty.channel.{ ChannelHandler, SimpleChannelDownstreamHandler, ChannelHandlerContext, MessageEvent, Channels, ChannelFutureListener }
import io.netty.buffer.ChannelBuffers
import ChannelHandler.Sharable
import io.netty.handler.codec.http.{ DefaultHttpResponse, HttpHeaders, HttpRequest, HttpResponse, HttpResponseStatus, HttpVersion }
import HttpResponseStatus.OK
import HttpHeaders.Names.{ CONTENT_ENCODING, CONTENT_TYPE }

import xitrum.{ Cache, Config, Logger }
import xitrum.Controller
import xitrum.routing.Routes
import xitrum.scope.request.RequestEnv
import xitrum.handler.HandlerEnv
import xitrum.util.{ Gzip, Mime }

object ResponseCacher extends Logger {
  //                             statusCode  headers                  content
  private type CachedResponse = (Int, Array[(String, String)], Array[Byte])

  def cacheResponse(controller: Controller) {
    val cacheSeconds = controller.handlerEnv.route.cacheSeconds
    if (cacheSeconds == 0) return

    val key = makeCacheKey(controller)
    if (!Cache.cache.containsKey(key)) { // Check to avoid the cost of serializing
      val response = controller.response
      val cachedResponse = serializeResponse(controller.request, response)
      val positiveCacheSeconds = if (cacheSeconds < 0) -cacheSeconds else cacheSeconds
      Cache.putIfAbsentSecond(key, cachedResponse, positiveCacheSeconds)
    }
  }

  def getCachedResponse(controller: Controller): Option[HttpResponse] = {
    val key = makeCacheKey(controller)
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
  private def makeCacheKey(controller: Controller): String = {
    // See xitrum.scope.request.Params in xitrum/scope/request/package.scala
    val sortedMap = (new TreeMap[String, List[String]]) ++ controller.textParams

    val request = controller.request
    val key =
      Cache.pageActionPrefix(controller) + "/" +
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

    val env = m.asInstanceOf[HandlerEnv]
    val controller = env.controller

    // controller may be null when the request could not go to Dispatcher, for
    // example when the response is served from PublicResourceServer
    if (controller == null) {
      ctx.sendDownstream(e)
      return
    }

    val response = controller.response
    if (response.getStatus == OK && !response.isChunked) cacheResponse(controller)

    ctx.sendDownstream(e)
  }
}
