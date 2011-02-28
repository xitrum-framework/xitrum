package xitrum.handler.up

import java.lang.reflect.{Method, InvocationTargetException}
import java.io.Serializable
import java.util.{Map => JMap, List => JList, LinkedHashMap => JLinkedHashMap}
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
import xitrum.action.env.{Env => AEnv}
import xitrum.action.exception.MissingParam
import xitrum.action.routing.{Routes, POST2Action, Util}
import xitrum.handler.Env
import xitrum.handler.down.ResponseCacher
import xitrum.handler.updown.XSendfile

object Dispatcher extends Logger {


  def dispatchWithFailsafe(action: Action) {
    val beginTimestamp = System.currentTimeMillis
    var hit            = false

    try {
      action.henv.action = action
      val actionClass = action.getClass.asInstanceOf[Class[Action]]
      val cacheSecs   = Routes.getCacheSecs(actionClass)

      def tryCache(f: => Unit) {
        val key   = ResponseCacher.makeCacheKey(action)
        val value = Cache.cache.get(key)
        if (value == null) {
          f  // hit = false, the
        } else {
          hit = true
          val response = ResponseCacher.deserializeToResponse(value.asInstanceOf[Serializable])
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
          response.setHeader(XSendfile.XSENDFILE_HEADER, System.getProperty("user.dir") + "/public/500.html")
        } else {
          logAccess(action, beginTimestamp, 0, false)
        }

        action.henv.response = response
        action.ctx.getChannel.write(action.henv)
    }
  }

  //----------------------------------------------------------------------------

  private def logAccess(action: Action, beginTimestamp: Long, cacheSecs: Int, hit: Boolean, e: Throwable = null) {
    // POST2Action is a gateway, skip it to avoid noisy log if there is no error
    if (action.isInstanceOf[POST2Action] && e == null) return

    def msgWithTime = {
      val endTimestamp = System.currentTimeMillis
      val dt           = endTimestamp - beginTimestamp

      action.request.getMethod                                                                    +
      " " + action.pathInfo.decoded                                                               +
      (if (!action.uriParams.isEmpty)  ", uriParams: "  + filterParams(action.uriParams)  else "") +
      (if (!action.bodyParams.isEmpty) ", bodyParams: " + filterParams(action.bodyParams) else "") +
      (if (!action.pathParams.isEmpty) ", pathParams: " + filterParams(action.pathParams) else "") +
      (if (!action.fileParams.isEmpty) ", fileParams: " +              action.fileParams  else "") +
      ", " + dt + " [ms]"
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
    for (key <- Config.filteredParams) {
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
      case Some((method, actionClass, pathParams)) =>
        request.setMethod(method)  // Override
        env.pathParams  = pathParams

        val action = actionClass.newInstance
        action(ctx, env)
        dispatchWithFailsafe(action)

      case None =>
        val response = new DefaultHttpResponse(HTTP_1_1, NOT_FOUND)
        response.setHeader(XSendfile.XSENDFILE_HEADER, System.getProperty("user.dir") + "/public/404.html")
        env.response = response
        ctx.getChannel.write(env)
    }
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, e: ExceptionEvent) {
    logger.error("Dispatcher", e.getCause)
    e.getChannel.close
  }
}
