package xitrum.handler.outbound

import scala.collection.immutable.SortedMap
import scala.collection.mutable.{Map => MMap}

import io.netty.buffer.Unpooled
import io.netty.handler.codec.http.{DefaultFullHttpResponse, HttpHeaders, FullHttpResponse, HttpResponseStatus, HttpVersion}
import HttpResponseStatus.OK

import xitrum.Config
import xitrum.Action
import xitrum.handler.HandlerEnv
import xitrum.scope.request.Params
import xitrum.util.Gzip

object ResponseCacher {
  //                             statusCode  headers           content
  private type CachedResponse = (Int, Array[(String, String)], Array[Byte])

  def shouldCache(env: HandlerEnv): Boolean = {
    val route    = env.route
    val response = env.response

    // route may be null when the request could not go to Dispatcher, for
    // example when the response is served from PublicResourceServer
    route != null &&
    route.cacheSecs != 0 &&
    response.getStatus == OK &&
    !HttpHeaders.isTransferEncodingChunked(response)
  }

  def cacheResponse(env: HandlerEnv) {
    val actionClass = env.route.klass
    val urlParams   = env.urlParams
    val gzipped     = Gzip.isAccepted(env.request)
    val key         = makeCacheKey(actionClass, urlParams, gzipped)
    val cache       = Config.xitrum.cache
    if (!cache.isDefinedAt(key)) {  // Check to avoid the cost of serializing
      val response          = env.response
      val cachedResponse    = serializeResponse(gzipped, response)
      val cacheSecs         = env.route.cacheSecs
      val positiveCacheSecs = if (cacheSecs < 0) -cacheSecs else cacheSecs
      cache.putSecondIfAbsent(key, cachedResponse, positiveCacheSecs)
    }
  }

  def getCachedResponse(env: HandlerEnv): Option[FullHttpResponse] = {
    val actionClass = env.route.klass
    val urlParams   = env.urlParams
    val gzipped     = Gzip.isAccepted(env.request)
    val key         = makeCacheKey(actionClass, urlParams, gzipped)
    val cache       = Config.xitrum.cache
    cache.getAs[CachedResponse](key).map(deserializeToResponse)
  }

  def removeCachedResponse(actionClass: Class[Action], urlParams: Params) {
    val cache = Config.xitrum.cache

    val keyTrue = makeCacheKey(actionClass, urlParams, true)
    cache.remove(keyTrue)

    val keyFalse = makeCacheKey(actionClass, urlParams, false)
    cache.remove(keyFalse)
  }

  def removeCachedResponse(actionClass: Class[Action], urlParams: (String, Any)*) {
    val params = MMap.empty[String, Seq[String]]
    urlParams.foreach { case (k, v) => params += (k -> Seq(v.toString)) }
    removeCachedResponse(actionClass, params)
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
  private def serializeResponse(gzipAccepted: Boolean, response: FullHttpResponse): CachedResponse = {
    val status = response.getStatus.code

    // Should be before extracting headers, because the CONTENT_LENGTH header
    // can be updated if the content if gzipped
    val bytes = Gzip.tryCompressBigTextualResponse(gzipAccepted, response, true)

    val headers = {
      val list = response.headers.entries  // JList[JMap.Entry[String, String]], JMap.Entry is not Serializable!
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

  private def deserializeToResponse(cachedResponse: CachedResponse): FullHttpResponse = {
    val (status, headers, bytes) = cachedResponse
    val response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.valueOf(status), Unpooled.wrappedBuffer(bytes))
    for ((k, v) <- headers) HttpHeaders.addHeader(response, k, v)
    response
  }

  /**
   * Only route action class, urlParams, and gzipped is included in the key,
   * because responses of requests other than GET requests should be cached.
   */
  private def makeCacheKey(actionClass: Class[_], urlParams: Params, gzipped: Boolean): String = {
    // See xitrum.scope.request.Params in xitrum/scope/request/package.scala
    // Need to sort by keys so that the output is consistent
    val sortedMap = SortedMap.empty[String, Seq[String]] ++ urlParams

    val key =
      "xitrum/page-action/" +
      actionClass.getName + "/" +
      sortedMap.toString
    if (gzipped) key + "_gzipped" else key
  }
}
