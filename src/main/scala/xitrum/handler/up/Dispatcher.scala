package xitrum.handler.up

import java.io.Serializable
import scala.collection.mutable.{ArrayBuffer, Map => MMap}

import org.jboss.netty.channel._
import org.jboss.netty.buffer.ChannelBuffers
import org.jboss.netty.util.CharsetUtil
import org.jboss.netty.handler.codec.http._
import ChannelHandler.Sharable
import HttpResponseStatus._
import HttpVersion._

import xitrum.{Action, SkipCSRFCheck, Cache, Config, Logger}
import xitrum.routing.{Routes, PostbackAction}
import xitrum.exception.{InvalidAntiCSRFToken, MissingParam}
import xitrum.handler.HandlerEnv
import xitrum.handler.down.ResponseCacher
import xitrum.handler.updown.XSendfile
import xitrum.scope.request.RequestEnv
import xitrum.scope.session.CSRF

object Dispatcher extends Logger {
  def dispatchWithFailsafe(action: Action, postback: Boolean) {
    val beginTimestamp = System.currentTimeMillis
    var hit            = false

    try {
      action.handlerEnv.action = action

      // Check for CSRF (CSRF has been checked if "postback" is true)
      if (!postback &&
          action.request.getMethod() != HttpMethod.GET &&
          !action.isInstanceOf[SkipCSRFCheck] &&
          !CSRF.isValidToken(action)) throw new InvalidAntiCSRFToken

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

        // These exceptions are special cases:
        // We know that the exception is caused by the client (bad request)
        if (e.isInstanceOf[InvalidAntiCSRFToken] || e.isInstanceOf[MissingParam]) {
          logAccess(action, postback, beginTimestamp, 0, false)

          action.response.setStatus(BAD_REQUEST)

          if (e.isInstanceOf[InvalidAntiCSRFToken]) {
            action.session.reset
            action.jsRenderCall("alert", "\"Session expired. Please refresh your browser.\"")
          } else if (e.isInstanceOf[MissingParam]) {
            val mp  = e.asInstanceOf[MissingParam]
            val key = mp.key
            action.renderText("Missing Param: " + key)
          }
        } else {
          logAccess(action, postback, beginTimestamp, 0, false, e)

          val response = new DefaultHttpResponse(HTTP_1_1, INTERNAL_SERVER_ERROR)
          XSendfile.set500Page(response)
          action.handlerEnv.response = response
          action.ctx.getChannel.write(action.handlerEnv)
        }
    }
  }

  //----------------------------------------------------------------------------

  private def logAccess(action: Action, postback: Boolean, beginTimestamp: Long, cacheSecs: Int, hit: Boolean, e: Throwable = null) {
    // PostbackAction is a gateway, skip it to avoid noisy log if there is no error
    if (action.isInstanceOf[PostbackAction] && e == null) return

    def msgWithTime = {
      val endTimestamp = System.currentTimeMillis
      val dt           = endTimestamp - beginTimestamp

      (if (postback) "POSTBACK" else action.request.getMethod) + " " + action.getClass.getName                                                                                                         +
      (if (!action.handlerEnv.uriParams.isEmpty)        ", uriParams: "        + RequestEnv.inspectParamsWithFilter(action.handlerEnv.uriParams.asInstanceOf[MMap[String, List[Any]]])        else "") +
      (if (!action.handlerEnv.bodyParams.isEmpty)       ", bodyParams: "       + RequestEnv.inspectParamsWithFilter(action.handlerEnv.bodyParams.asInstanceOf[MMap[String, List[Any]]])       else "") +
      (if (!action.handlerEnv.pathParams.isEmpty)       ", pathParams: "       + RequestEnv.inspectParamsWithFilter(action.handlerEnv.pathParams.asInstanceOf[MMap[String, List[Any]]])       else "") +
      (if (!action.handlerEnv.fileUploadParams.isEmpty) ", fileUploadParams: " + RequestEnv.inspectParamsWithFilter(action.handlerEnv.fileUploadParams.asInstanceOf[MMap[String, List[Any]]]) else "") +
      ", " + dt + " [ms]"
    }

    def extraInfo = {
      if (cacheSecs == 0) {
        if (action.responded) "" else " (async)"
      } else {
        if (hit) {
          if (cacheSecs < 0) " (action cache hit)"  else " (page cache hit)"
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
class Dispatcher extends SimpleChannelUpstreamHandler with BadClientSilencer {
  import Dispatcher._

  override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {
    val m = e.getMessage
    if (!m.isInstanceOf[HandlerEnv]) {
      ctx.sendUpstream(e)
      return
    }

    val env        = m.asInstanceOf[HandlerEnv]
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
        XSendfile.set404Page(response)
        env.response = response
        ctx.getChannel.write(env)
    }
  }

  //----------------------------------------------------------------------------

  private val closedListeners = ArrayBuffer[() => Unit]()

  def addConnectionClosedListener(listener: () => Unit) {
    closedListeners.synchronized { closedListeners.append(listener) }
  }

  override def channelClosed(ctx: ChannelHandlerContext, e: ChannelStateEvent) {
    closedListeners.synchronized {
      closedListeners.foreach { listener => listener() }
    }
  }
}
