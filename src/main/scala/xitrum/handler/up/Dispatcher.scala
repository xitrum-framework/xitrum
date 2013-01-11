package xitrum.handler.up

import java.lang.reflect.Method
import scala.collection.mutable.{ArrayBuffer, Map => MMap}

import org.jboss.netty.channel._
import org.jboss.netty.handler.codec.http._
import ChannelHandler.Sharable
import HttpResponseStatus._
import HttpVersion._

import xitrum.{Config, Controller, SkipCSRFCheck, Cache, Logger}
import xitrum.controller.Action
import xitrum.exception.{InvalidAntiCSRFToken, InvalidInput, MissingParam, SessionExpired}
import xitrum.handler.{AccessLog, HandlerEnv}
import xitrum.handler.down.{ResponseCacher, XSendFile}
import xitrum.routing.{ControllerReflection, Routes}
import xitrum.scope.request.RequestEnv
import xitrum.scope.session.CSRF
import xitrum.sockjs.SockJsController

object Dispatcher extends Logger {
  def dispatchWithFailsafe(actionMethod: Method, env: HandlerEnv) {
    val beginTimestamp = System.currentTimeMillis()
    var hit            = false

    val (controller, withActionMethod) = ControllerReflection.newControllerAndAction(actionMethod)
    controller(env)

    // Set pathPrefix for SockJsController
    if (controller.isInstanceOf[SockJsController])
      controller.pathPrefix = controller.pathInfo.tokens(0)

    env.action     = withActionMethod
    env.controller = controller

    try {
      // Check for CSRF (CSRF has been checked if "postback" is true)
      if ((controller.request.getMethod == HttpMethod.POST ||
           controller.request.getMethod == HttpMethod.PUT ||
           controller.request.getMethod == HttpMethod.DELETE) &&
          !controller.isInstanceOf[SkipCSRFCheck] &&
          !CSRF.isValidToken(controller)) throw new InvalidAntiCSRFToken

      val cacheSeconds = withActionMethod.cacheSeconds

      if (cacheSeconds > 0) {     // Page cache
        tryCache(controller) {
          val passed = controller.callBeforeFilters()
          if (passed) runAroundAndAfterFilters(controller, withActionMethod)
        }
      } else {
        val passed = controller.callBeforeFilters()
        if (passed) {
          if (cacheSeconds < 0)  // Action cache
            tryCache(controller) { runAroundAndAfterFilters(controller, withActionMethod) }
          else                   // No cache
            runAroundAndAfterFilters(controller, withActionMethod)
        }
      }

      AccessLog.logDynamicContentAccess(controller, beginTimestamp, cacheSeconds, hit)
    } catch {
      case scala.util.control.NonFatal(e) =>
        // End timestamp
        val t2 = System.currentTimeMillis()

        // These exceptions are special cases:
        // We know that the exception is caused by the client (bad request)
        if (e.isInstanceOf[SessionExpired] || e.isInstanceOf[InvalidAntiCSRFToken] || e.isInstanceOf[MissingParam] || e.isInstanceOf[InvalidInput]) {
          controller.response.setStatus(BAD_REQUEST)
          val msg = if (e.isInstanceOf[SessionExpired] || e.isInstanceOf[InvalidAntiCSRFToken]) {
            controller.session.clear()
            "Session expired. Please refresh your browser."
          } else if (e.isInstanceOf[MissingParam]) {
            val mp  = e.asInstanceOf[MissingParam]
            "Missing param: " + mp.key
          } else {
            val ve = e.asInstanceOf[InvalidInput]
            "Validation error: " + ve.message
          }

          if (controller.isAjax)
            controller.jsRespond("alert(\"" + controller.jsEscape(msg) + "\")")
          else
            controller.respondText(msg)

          AccessLog.logDynamicContentAccess(controller, beginTimestamp, 0, false)
        } else {
          controller.response.setStatus(INTERNAL_SERVER_ERROR)
          if (Config.productionMode) {
            Routes.action500Method match {
              case None => respondDefault500AlertOrPage(controller)

              case Some(action500Method) =>
                if (action500Method == actionMethod) {
                  respondDefault500AlertOrPage(controller)
                } else {
                  controller.response.setStatus(INTERNAL_SERVER_ERROR)
                  dispatchWithFailsafe(action500Method, env)
                }
            }
          } else {
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

            if (controller.isAjax)
              controller.jsRespond("alert(\"" + controller.jsEscape(errorMsg) + "\")")
            else
              controller.respondText(errorMsg)
          }

          AccessLog.logDynamicContentAccess(controller, beginTimestamp, 0, false, e)
        }
    }
  }

  //----------------------------------------------------------------------------

  private def respondDefault500AlertOrPage(controller: Controller) {
    if (controller.isAjax) {
      controller.jsRespond("alert(\"" + controller.jsEscape("Internal Server Error") + "\")")
    } else {
      val response = new DefaultHttpResponse(HTTP_1_1, INTERNAL_SERVER_ERROR)
      XSendFile.set500Page(response, true)
      val env = controller.handlerEnv
      env.response = response
      env.channel.write(env)
    }
  }

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

  private def runAroundAndAfterFilters(controller: Controller, action: Action) {
    controller.callAroundFilters(action)
    controller.callAfterFilters()
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

    // Look up GET if method is HEAD
    val requestMethod = if (request.getMethod == HttpMethod.HEAD) HttpMethod.GET else request.getMethod
    Routes.matchRoute(requestMethod, pathInfo) match {
      case Some((actionMethod, pathParams)) =>
        env.pathParams = pathParams
        dispatchWithFailsafe(actionMethod, env)

      case None =>
        Routes.action404Method match {
          case None =>
            val response = new DefaultHttpResponse(HTTP_1_1, NOT_FOUND)
            XSendFile.set404Page(response, false)
            env.response = response
            ctx.getChannel.write(env)

          case Some(actionMethod) =>
            env.pathParams = MMap.empty
            env.response.setStatus(NOT_FOUND)
            dispatchWithFailsafe(actionMethod, env)
        }
    }
  }
}
