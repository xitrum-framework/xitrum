package xitrum.handler.up

import java.lang.reflect.Method
import scala.collection.mutable.{ArrayBuffer, Map => MMap}

import io.netty.channel._
import io.netty.handler.codec.http._
import ChannelHandler.Sharable
import HttpResponseStatus._
import HttpVersion._

import xitrum.{Config, Controller, SkipCSRFCheck, Cache, Logger}
import xitrum.routing.{Route, Routes, PostbackController}
import xitrum.exception.{InvalidAntiCSRFToken, MissingParam, SessionExpired}
import xitrum.handler.HandlerEnv
import xitrum.handler.down.ResponseCacher
import xitrum.handler.down.XSendFile
import xitrum.routing.{ControllerReflection, HttpMethodWebSocket}
import xitrum.scope.request.RequestEnv
import xitrum.scope.session.CSRF

object Dispatcher extends Logger {
  def dispatchWithFailsafe(route: Route, env: HandlerEnv, postback: Boolean) {
    val beginTimestamp = System.currentTimeMillis()
    var hit            = false

    val (controller, withRouteMethod) = ControllerReflection.newControllerAndRoute(route)
    controller(env)
    controller.setPostback(postback)

    env.route      = withRouteMethod
    env.controller = controller
    try {
      // Check for CSRF (CSRF has been checked if "postback" is true)
      if (!postback &&
          controller.request.getMethod != HttpMethod.GET &&
          controller.request.getMethod != HttpMethodWebSocket &&
          !controller.isInstanceOf[SkipCSRFCheck] &&
          !CSRF.isValidToken(controller)) throw new InvalidAntiCSRFToken

      val cacheSeconds = withRouteMethod.cacheSeconds

      if (cacheSeconds > 0) {             // Page cache
        tryCache(controller) {
          val passed = controller.callBeforeFilters()
          if (passed) runAroundAndAfterFilters(controller, withRouteMethod)
        }
      } else {
        val passed = controller.callBeforeFilters()
        if (passed) {
          if (cacheSeconds == 0) {        // No cache
            runAroundAndAfterFilters(controller, withRouteMethod)
          } else if (cacheSeconds < 0) {  // Action cache
            tryCache(controller) { runAroundAndAfterFilters(controller, withRouteMethod) }
          }
        }
      }

      logAccess(controller, postback, beginTimestamp, cacheSeconds, hit)
    } catch {
      case e =>
        // End timestamp
        val t2 = System.currentTimeMillis()

        // These exceptions are special cases:
        // We know that the exception is caused by the client (bad request)
        if (e.isInstanceOf[SessionExpired] || e.isInstanceOf[InvalidAntiCSRFToken] || e.isInstanceOf[MissingParam]) {
          logAccess(controller, postback, beginTimestamp, 0, false)

          controller.response.setStatus(BAD_REQUEST)
          val msg = if (e.isInstanceOf[SessionExpired] || e.isInstanceOf[InvalidAntiCSRFToken]) {
            controller.resetSession()
            "Session expired. Please refresh your browser."
          } else if (e.isInstanceOf[MissingParam]) {
            val mp  = e.asInstanceOf[MissingParam]
            val key = mp.key
            "Missing param: " + key
          }
          if (controller.isAjax)
            controller.jsRespond("alert(" + controller.jsEscape(msg) + ")")
          else
            controller.respondText(msg)
        } else {
          logAccess(controller, postback, beginTimestamp, 0, false, e)

          if (Config.isProductionMode) {
            if (controller.isAjax) {
              controller.response.setStatus(INTERNAL_SERVER_ERROR)
              controller.jsRespond("alert(" + controller.jsEscape("Internal Server Error") + ")")
            } else {
              if (Routes.error500 == null || Routes.error500 == route) {
                val response = new DefaultHttpResponse(HTTP_1_1, INTERNAL_SERVER_ERROR)
                XSendFile.set500Page(response)
                env.response = response
                env.channel.write(env)
              } else {
                controller.response.setStatus(INTERNAL_SERVER_ERROR)
                controller.forward(Routes.error500, false)
              }
            }
          } else {
            controller.response.setStatus(INTERNAL_SERVER_ERROR)

            val normalErrorMsg = e.toString + "\n\n" + e.getStackTraceString
            val errorMsg = if (e.isInstanceOf[org.fusesource.scalate.InvalidSyntaxException]) {
              val ise = e.asInstanceOf[org.fusesource.scalate.InvalidSyntaxException]
              val pos = ise.pos
              "Scalate syntax error: " + ise.source.uri + ", line " + pos.line + "\n" +
              pos.longString + "\n\n" +
              normalErrorMsg
            } else {
              normalErrorMsg
            }

            if (controller.isAjax) {
              controller.jsRespond("alert(" + controller.jsEscape(errorMsg) + ")")
            } else {
              controller.respondText(errorMsg)
            }
          }
        }
    }
  }

  //----------------------------------------------------------------------------

  /** @return true if the cache was hit */
  private def tryCache(controller: Controller)(f: => Unit): Boolean = {
    ResponseCacher.getCachedResponse(controller) match {
      case None =>
        f
        false

      case Some(response) =>
        controller.channel.write(response)
        true
    }
  }

  private def runAroundAndAfterFilters(controller: Controller, route: Route) {
    controller.callAroundFilters(route)
    controller.callAfterFilters()
  }

  private def logAccess(controller: Controller, postback: Boolean, beginTimestamp: Long, cacheSecs: Int, hit: Boolean, e: Throwable = null) {
    // PostbackAction is a gateway, skip it to avoid noisy log if there is no error
    if (controller.isInstanceOf[PostbackController] && e == null) return

    def msgWithTime = {
      val endTimestamp = System.currentTimeMillis()
      val dt           = endTimestamp - beginTimestamp
      val env          = controller.handlerEnv

      (if (postback) "POSTBACK" else controller.request.getMethod) + " " + ControllerReflection.controllerRouteName(controller.handlerEnv.route)                           +
      (if (!env.uriParams.isEmpty)        ", uriParams: "        + RequestEnv.inspectParamsWithFilter(env.uriParams       .asInstanceOf[MMap[String, List[Any]]]) else "") +
      (if (!env.bodyParams.isEmpty)       ", bodyParams: "       + RequestEnv.inspectParamsWithFilter(env.bodyParams      .asInstanceOf[MMap[String, List[Any]]]) else "") +
      (if (!env.pathParams.isEmpty)       ", pathParams: "       + RequestEnv.inspectParamsWithFilter(env.pathParams      .asInstanceOf[MMap[String, List[Any]]]) else "") +
      (if (!env.fileUploadParams.isEmpty) ", fileUploadParams: " + RequestEnv.inspectParamsWithFilter(env.fileUploadParams.asInstanceOf[MMap[String, List[Any]]]) else "") +
      ", " + dt + " [ms]"
    }

    def extraInfo = {
      if (cacheSecs == 0) {
        if (controller.isResponded) "" else " (async)"
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
      case Some((route, pathParams)) =>
        env.pathParams = pathParams
        dispatchWithFailsafe(route, env, false)

      case None =>
        if (Routes.error404 == null) {
          val response = new DefaultHttpResponse(HTTP_1_1, NOT_FOUND)
          XSendFile.set404Page(response)
          env.response = response
          ctx.getChannel.write(env)
        } else {
          env.pathParams = MMap.empty
          env.response.setStatus(NOT_FOUND)
          dispatchWithFailsafe(Routes.error404, env, false)
        }
    }
  }
}
