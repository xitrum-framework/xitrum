package xitrum.handler.up

import java.io.Serializable
import scala.collection.mutable.{Map => MMap}

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
import xitrum.action.routing.{Routes, PostbackAction}
import xitrum.handler.Env
import xitrum.handler.down.ResponseCacher
import xitrum.handler.updown.XSendfile

object Dispatcher extends Logger {
  def dispatchWithFailsafe(action: Action, postback: Boolean) {
    val beginTimestamp = System.currentTimeMillis
    var hit            = false

    try {
      action.henv.action = action
      val actionClass = action.getClass.asInstanceOf[Class[Action]]
      val cacheSecs   = Routes.getCacheSecs(actionClass)

      def tryCache(f: => Unit) {
        ResponseCacher.getCachedResponse(action) match {
          case None =>
            f  // hit has already been initialized to false

          case Some(response) =>
            hit = true
            action.ctx.getChannel.write(response)
        }
      }

      if (cacheSecs > 0) {             // Page cache
        tryCache {
          val passed = action.callBeforeFilters
          if (passed) {
            if (postback) action.postback else action.execute
          }
        }
      } else {
        val passed = action.callBeforeFilters
        if (passed) {
          if (cacheSecs == 0) {        // No cache
            if (postback) action.postback else action.execute
          } else if (cacheSecs < 0) {  // Action cache
            tryCache { if (postback) action.postback else action.execute }
          }
        }
      }

      logAccess(action, postback, beginTimestamp, cacheSecs, hit)
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
          logAccess(action, postback, beginTimestamp, 0, false, e)
          response.setHeader(XSendfile.XSENDFILE_HEADER, System.getProperty("user.dir") + "/public/500.html")
        } else {
          logAccess(action, postback, beginTimestamp, 0, false)
        }

        action.henv.response = response
        action.ctx.getChannel.write(action.henv)
    }
  }

  //----------------------------------------------------------------------------

  private def logAccess(action: Action, postback: Boolean, beginTimestamp: Long, cacheSecs: Int, hit: Boolean, e: Throwable = null) {
    // PostbackAction is a gateway, skip it to avoid noisy log if there is no error
    if (action.isInstanceOf[PostbackAction] && e == null) return

    def msgWithTime = {
      val endTimestamp = System.currentTimeMillis
      val dt           = endTimestamp - beginTimestamp

      (if (postback) "POSTBACK " + action.getClass.getName else action.request.getMethod + " " + action.pathInfo.decoded)                                               +
      (if (!action.uriParams.isEmpty)        ", uriParams: "        + AEnv.inspectParams(action.uriParams       .asInstanceOf[MMap[String, Array[Any]]]) else "") +
      (if (!action.bodyParams.isEmpty)       ", bodyParams: "       + AEnv.inspectParams(action.bodyParams      .asInstanceOf[MMap[String, Array[Any]]]) else "") +
      (if (!action.pathParams.isEmpty)       ", pathParams: "       + AEnv.inspectParams(action.pathParams      .asInstanceOf[MMap[String, Array[Any]]]) else "") +
      (if (!action.fileUploadParams.isEmpty) ", fileUploadParams: " + AEnv.inspectParams(action.fileUploadParams.asInstanceOf[MMap[String, Array[Any]]]) else "") +
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

    if (e == null) {
      if (logger.isDebugEnabled) logger.debug(msgWithTime + extraInfo)
    } else {
      if (logger.isErrorEnabled) logger.error("Dispatching error " + msgWithTime + extraInfo, e)
    }
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
        dispatchWithFailsafe(action, false)

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
