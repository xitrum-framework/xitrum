package xitrum.handler.up

import java.lang.reflect.{Method, InvocationTargetException}
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
    // Begin timestamp
    val beginTimestamp = System.currentTimeMillis

    try {
      def tryCache(f: => Unit) {
        val key   = paramsToKey(action.allParams)
        val value = Cache.cache.get(key)
        if (value == null) {
          val msg = if (cacheSecs < 0) "Action cache miss" else "Page cache miss"
          logger.debug(msg)

          f

          val secs = if (cacheSecs < 0) -cacheSecs else cacheSecs
          Cache.cache.put(key, action.response, secs, TimeUnit.SECONDS)
          action.ctx.getChannel.write(action.response)
        } else {
          val msg = if (cacheSecs < 0) "Action cache hit" else "Page cache hit"
          logger.debug(msg)
          action.ctx.getChannel.write(value)
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

      logAccess(beginTimestamp, action)
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
          logAccess(beginTimestamp, action, e)
          response.setHeader("X-Sendfile", System.getProperty("user.dir") + "/public/500.html")
        } else {
          logAccess(beginTimestamp, action)
        }

        action.henv.response = response
        action.ctx.getChannel.write(action.henv)
    }
  }

  //----------------------------------------------------------------------------

  def paramsToKey(params: AEnv.Params): String = {
    val sortedMap = new TreeMap[String, JList[String]](params)
    sortedMap.toString
  }

  private def logAccess(beginTimestamp: Long, action: Action, e: Throwable = null) {
    // POST2Action is a gateway, skip it to avoid noisy log if there is no error
    if (action.isInstanceOf[POST2Action] && e == null) return

    val endTimestamp = System.currentTimeMillis
    val dt           = endTimestamp - beginTimestamp

    val async = if (action.responded) "" else " (async)"
    if (e == null) {
      val msg =
        action.request.getMethod       + " " +
        action.pathInfo.decoded        + " " +
        filterParams(action.allParams) + " " +
        dt + " [ms]" + async
      logger.debug(msg)
    } else {
      val msg =
        "Dispatching error " +
        action.request.getMethod       + " " +
        action.pathInfo.decoded        + " " +
        filterParams(action.allParams) + " " +
        dt + " [ms]" + async
      logger.error(msg, e)
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
