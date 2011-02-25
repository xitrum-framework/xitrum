package xitrum.handler.down

import java.io.Serializable
import java.util.{Map => JMap, List => JList, LinkedHashMap, TreeMap}
import java.util.concurrent.TimeUnit

import org.jboss.netty.channel.{ChannelHandler, SimpleChannelDownstreamHandler, ChannelHandlerContext, MessageEvent, Channels, ChannelFutureListener}
import org.jboss.netty.buffer.ChannelBuffers
import ChannelHandler.Sharable
import org.jboss.netty.handler.codec.http.{DefaultHttpResponse, HttpHeaders, HttpResponse, HttpResponseStatus, HttpVersion}

import xitrum.Cache
import xitrum.action.Action
import xitrum.action.routing.Routes
import xitrum.handler.Env

object ResponseCacher {
  def makeCacheKey(action: Action): String = {
    val params    = action.allParams
    val sortedMap = new TreeMap[String, JList[String]](params)
    "page/action cache/" + action.getClass.getName + "/" + sortedMap.toString
  }

    /**
   * Response can be (re)constructed from (status, headers, content).
   * To be stored in cache, these must be Serializable. We choose:
   *   status:  Int
   *   headers: Array[(String, String)]
   *   content: Array[Byte]
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
    val bytes   = {
      val channelBuffer = response.getContent
      val ret = new Array[Byte](channelBuffer.readableBytes)
      channelBuffer.readBytes(ret)
      channelBuffer.resetReaderIndex
      ret
    }
    (status, headers, bytes).asInstanceOf[Serializable]
  }

  /** This is the reverse of serializeResponse. */
  def deserializeToResponse(serializable: Serializable): HttpResponse = {
    val (status, headers, bytes) = serializable.asInstanceOf[(Int, Array[(String, String)], Array[Byte])]

    val response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.valueOf(status))
    for ((k, v) <- headers) response.addHeader(k, v)
    response.setContent(ChannelBuffers.wrappedBuffer(bytes))

    response
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
