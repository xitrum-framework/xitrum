package xitrum.handler.up

import java.lang.reflect.{Method, InvocationTargetException}
import java.io.Serializable
import java.util.{Map => JMap, List => JList, LinkedHashMap, TreeMap}
import java.util.concurrent.TimeUnit

import org.jboss.netty.channel._
import org.jboss.netty.buffer.ChannelBuffers
import org.jboss.netty.util.CharsetUtil
import org.jboss.netty.handler.codec.http._
import ChannelHandler.Sharable
import HttpResponseStatus._
import HttpVersion._

import xitrum.{Cache, Config, Logger}
import xitrum.action.Action
import xitrum.handler.Env
import xitrum.action.env.{Env => AEnv}
import xitrum.action.exception.MissingParam
import xitrum.action.routing.{Routes, POST2Action, Util}

object Dispatcher extends Logger {
  def dispatchWithFailsafe(action: Action, cacheSecs: Int = 0) {
    val beginTimestamp = System.currentTimeMillis
    var hit            = false

    try {
      def tryCache(f: => Unit) {
        val key   = makeCacheKey(action)
        val value = Cache.cache.get(key)
        val actionClassName = action.getClass.getName
        if (value == null) {
          f  // hit = false

          val serializable = serializeResponse(action.response)
          val secs = if (cacheSecs < 0) -cacheSecs else cacheSecs
          Cache.cache.put(key, serializable, secs, TimeUnit.SECONDS)

          action.ctx.getChannel.write(action.response)
        } else {
          hit = true
          val response = deserializeToResponse(value.asInstanceOf[Serializable])
          action.ctx.getChannel.write(response)
        }
      }

      if (cacheSecs > 0) {             // Page cache
        tryCache {
          val passed = action.callBeforeFilters
          if (passed) action.execute
        }
      } else {
        val passed = action.callBeforeFilters
        if (passed) {
          if (cacheSecs == 0) {        // No cache
            action.execute
          } else if (cacheSecs < 0) {  // Action cache
            tryCache { action.execute }
          }
        }
      }

      logAccess(action, beginTimestamp, cacheSecs, hit)
    } catch {
      case e =>
        // End timestamp
        val t2 = System.currentTimeMillis

        val response = new DefaultHttpResponse(HTTP_1_1, INTERNAL_SERVER_ERROR)

        // MissingParam is a special case
        if (e.isInstanceOf[MissingParam]) {
          response.setStatus(BAD_REQUEST)
          val mp  = e.asInstanceOf[MissingParam]
          val key = mp.key
          val cb  = ChannelBuffers.copiedBuffer("Missing Param: " + key, CharsetUtil.UTF_8)
          HttpHeaders.setContentLength(response, cb.readableBytes)
          response.setContent(cb)
        }

        if (response.getStatus != BAD_REQUEST) {  // MissingParam
          logAccess(action, beginTimestamp, 0, false, e)
          response.setHeader("X-Sendfile", System.getProperty("user.dir") + "/public/500.html")
        } else {
          logAccess(action, beginTimestamp, 0, false)
        }

        action.henv.response = response
        action.ctx.getChannel.write(action.henv)
    }
  }

  //----------------------------------------------------------------------------

  def makeCacheKey(action: Action): String = {
    val params    = action.allParams
    val sortedMap = new TreeMap[String, JList[String]](params)
    action.getClass.getName + "/" + sortedMap.toString
  }

  /**
   * Response can be (re)constructed from (status, headers, content).
   * To be stored in cache, these must be Serializable. We choose:
   *   status:  Int
   *   headers: Array[(String, String)]
   *   content: Array[Byte]
   */
  private def serializeResponse(response: HttpResponse): Serializable = {
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
  private def deserializeToResponse(serializable: Serializable): HttpResponse = {
    val (status, headers, bytes) = serializable.asInstanceOf[(Int, Array[(String, String)], Array[Byte])]

    val response = new DefaultHttpResponse(HTTP_1_1, HttpResponseStatus.valueOf(status))
    for ((k, v) <- headers) response.addHeader(k, v)
    response.setContent(ChannelBuffers.wrappedBuffer(bytes))

    response
  }

  //----------------------------------------------------------------------------

  private def logAccess(action: Action, beginTimestamp: Long, cacheSecs: Int, hit: Boolean, e: Throwable = null) {
    // POST2Action is a gateway, skip it to avoid noisy log if there is no error
    if (action.isInstanceOf[POST2Action] && e == null) return

    def msgWithTime = {
      val endTimestamp = System.currentTimeMillis
      val dt           = endTimestamp - beginTimestamp

      action.request.getMethod       + " " +
      action.pathInfo.decoded        + " " +
      filterParams(action.allParams) + " " +
      dt + " [ms]"
    }

    def extraInfo = {
      if (cacheSecs == 0) {
        if (action.responded) "" else " (async)"
      } else {
        if (hit) {
          if (cacheSecs < 0) " (action cache hit)" else " (page cache hit)"
        } else {
          if (cacheSecs < 0) " (action cache miss)" else " (page cache miss)"
        }
      }
    }

    if (e == null && logger.isDebugEnabled) {
      logger.debug(msgWithTime + extraInfo)
    } else if (logger.isErrorEnabled){
      logger.error("Dispatching error " + msgWithTime + extraInfo, e)
    }
  }

  // Same as Rails' config.filter_parameters
  private def filterParams(params: java.util.Map[String, java.util.List[String]]): java.util.Map[String, java.util.List[String]] = {
    val ret = new java.util.LinkedHashMap[String, java.util.List[String]]()
    ret.putAll(params)
    for (key <- Config.filterParams) {
      if (ret.containsKey(key)) ret.put(key, Util.toValues("*FILTERED*"))
    }
    ret
  }
}

@Sharable
class Dispatcher extends SimpleChannelUpstreamHandler with ClosedClientSilencer {
  import Dispatcher._

  override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {
    val m = e.getMessage
    if (!m.isInstanceOf[Env]) {
      ctx.sendUpstream(e)
      return
    }

    val env        = m.asInstanceOf[Env]
    val request    = env.request
    val pathInfo   = env.pathInfo
    val uriParams  = env.uriParams
    val bodyParams = env.bodyParams

    Routes.matchRoute(request.getMethod, pathInfo) match {
      case Some((method, actionClass, pathParams, cacheSecs)) =>
        request.setMethod(method)  // Override
        env.pathParams = pathParams

        val action = actionClass.newInstance
        action(ctx, env)
        dispatchWithFailsafe(action, cacheSecs)

      case None =>
        val response = new DefaultHttpResponse(HTTP_1_1, NOT_FOUND)
        response.setHeader("X-Sendfile", System.getProperty("user.dir") + "/public/404.html")
        env.response = response
        ctx.getChannel.write(env)
    }
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, e: ExceptionEvent) {
    logger.error("Dispatcher", e.getCause)
    e.getChannel.close
  }
}
