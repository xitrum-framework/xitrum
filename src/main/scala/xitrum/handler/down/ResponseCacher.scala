package xitrum.handler.down

import scala.collection.immutable.SortedMap
import scala.collection.mutable.{Map => MMap}

import org.jboss.netty.channel.{ChannelHandler, SimpleChannelDownstreamHandler, ChannelHandlerContext, MessageEvent, Channels}
import org.jboss.netty.buffer.ChannelBuffers
import ChannelHandler.Sharable
import org.jboss.netty.handler.codec.http.{DefaultHttpResponse, HttpHeaders, HttpRequest, HttpResponse, HttpResponseStatus, HttpVersion}
import HttpResponseStatus.OK
import HttpHeaders.Names.{CONTENT_ENCODING, CONTENT_TYPE}

import xitrum.{Cache, Config, Log}
import xitrum.Action
import xitrum.scope.request.Params
import xitrum.handler.HandlerEnv
import xitrum.util.{Gzip, Mime}

object ResponseCacher extends Log {
  //                             statusCode  headers           content
  private type CachedResponse = (Int, Array[(String, String)], Array[Byte])

  def cacheResponse(env: HandlerEnv) {
    val actionClass = env.route.klass
    val urlParams   = env.urlParams
    val gzipped     = Gzip.isAccepted(env.request)
    val key         = makeCacheKey(actionClass, urlParams, gzipped)
    val cache       = Config.xitrum.cache
    if (!cache.isDefinedAt(key)) {  // Check to avoid the cost of serializing
      val response          = env.response
      val cachedResponse    = serializeResponse(env.request, response)
      val cacheSecs         = env.route.cacheSecs
      val positiveCacheSecs = if (cacheSecs < 0) -cacheSecs else cacheSecs
      cache.putSecondIfAbsent(key, cachedResponse, positiveCacheSecs)
    }
  }

  def getCachedResponse(env: HandlerEnv): Option[HttpResponse] = {
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
    val params = MMap[String, Seq[String]]()
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
  private def serializeResponse(request: HttpRequest, response: HttpResponse): CachedResponse = {
    val status = response.getStatus.getCode

    // Should be before extracting headers, because the CONTENT_LENGTH header
    // can be updated if the content if gzipped
    val bytes = Gzip.tryCompressBigTextualResponse(request, response)

    val headers = {
      val list = response.getHeaders  // JList[JMap.Entry[String, String]], JMap.Entry is not Serializable!
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

  /**
   * Only route action class, urlParams, and gzipped is included in the key,
   * because responses of requests other than GET requests should be cached.
   */
  private def makeCacheKey(actionClass: Class[_], urlParams: Params, gzipped: Boolean): String = {
    // See xitrum.scope.request.Params in xitrum/scope/request/package.scala
    // Need to sort by keys so that the output is consistent
    val sortedMap = SortedMap[String, Seq[String]]() ++ urlParams

    val key =
      "xitrum/page-action/" +
      actionClass.getName + "/" +
      sortedMap.toString
    if (gzipped) key + "_gzipped" else key
  }
}

@Sharable
class ResponseCacher extends SimpleChannelDownstreamHandler with Log {
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
    if (response.getStatus == OK && !response.isChunked && env.route.cacheSecs != 0)
      ResponseCacher.cacheResponse(env)

    ctx.sendDownstream(e)
  }
}
