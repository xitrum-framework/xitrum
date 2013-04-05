package xitrum.handler.up

import java.lang.reflect.Method
import scala.collection.mutable.{ArrayBuffer, Map => MMap}

import org.jboss.netty.channel._
import org.jboss.netty.handler.codec.http._
import ChannelHandler.Sharable
import HttpResponseStatus._
import HttpVersion._

import akka.actor.{Actor, ActorRef, Props}
import com.esotericsoftware.reflectasm.ConstructorAccess

import xitrum.{Action, Config, Cache, Logger, SkipCSRFCheck}
import xitrum.exception.{InvalidAntiCSRFToken, InvalidInput, MissingParam, SessionExpired}
import xitrum.handler.{AccessLog, HandlerEnv}
import xitrum.handler.down.{ResponseCacher, XSendFile}
import xitrum.routing.{Route, Routes}
import xitrum.scope.request.RequestEnv
import xitrum.scope.session.CSRF
import xitrum.sockjs.SockJsController

object Dispatcher extends Logger {
  def dispatchWithFailsafe(route: Route, env: HandlerEnv) {
    val beginTimestamp = System.currentTimeMillis()
    var hit            = false

    val (action, actorRef) = newActionActorRef(route.actionClass)
    actorRef ! env

    env.action = action

    try {
      // Check for CSRF (CSRF has been checked if "postback" is true)
      if ((action.request.getMethod == HttpMethod.POST ||
           action.request.getMethod == HttpMethod.PUT ||
           action.request.getMethod == HttpMethod.DELETE) &&
          !action.isInstanceOf[SkipCSRFCheck] &&
          !CSRF.isValidToken(action)) throw new InvalidAntiCSRFToken

      val cacheSeconds = route.cacheSeconds

      // Before filters:
      // When not passed, the before filters must explicitly respond to client,
      // with appropriate response status code, error description etc.
      // This logic is app-specific, Xitrum cannot does it for the app.

      if (cacheSeconds > 0) {     // Page cache
        hit = tryCache(action) {
          val passed = action.callBeforeFilters()
          if (passed) runAroundAndAfterFilters(action)
        }
      } else {
        val passed = action.callBeforeFilters()
        if (passed) {
          if (cacheSeconds < 0)  // Action cache
            hit = tryCache(action) { runAroundAndAfterFilters(action) }
          else                   // No cache
            runAroundAndAfterFilters(action)
        }
      }

      if (!action.forwarding) AccessLog.logDynamicContentAccess(action, beginTimestamp, cacheSeconds, hit)
    } catch {
      case scala.util.control.NonFatal(e) =>
        if (action.forwarding) return

        // End timestamp
        val t2 = System.currentTimeMillis()

        // These exceptions are special cases:
        // We know that the exception is caused by the client (bad request)
        if (e.isInstanceOf[SessionExpired] || e.isInstanceOf[InvalidAntiCSRFToken] || e.isInstanceOf[MissingParam] || e.isInstanceOf[InvalidInput]) {
          action.response.setStatus(BAD_REQUEST)
          val msg = if (e.isInstanceOf[SessionExpired] || e.isInstanceOf[InvalidAntiCSRFToken]) {
            action.session.clear()
            "Session expired. Please refresh your browser."
          } else if (e.isInstanceOf[MissingParam]) {
            val mp  = e.asInstanceOf[MissingParam]
            "Missing param: " + mp.key
          } else {
            val ve = e.asInstanceOf[InvalidInput]
            "Validation error: " + ve.message
          }

          if (action.isAjax)
            action.jsRespond("alert(\"" + action.jsEscape(msg) + "\")")
          else
            action.respondText(msg)

          AccessLog.logDynamicContentAccess(action, beginTimestamp, 0, false)
        } else {
          action.response.setStatus(INTERNAL_SERVER_ERROR)
          if (Config.productionMode) {
            Routes.error500 match {
              case None =>
                action.respondDefault500Page()

              case Some(action500) =>
                if (action500 == action) {
                  action.respondDefault500Page()
                } else {
                  action.response.setStatus(INTERNAL_SERVER_ERROR)
                  dispatchWithFailsafe(action500, env)
                }
            }
          } else {
            val errorMsg = e.toString + "\n\n" + e.getStackTraceString
            if (action.isAjax)
              action.jsRespond("alert(\"" + action.jsEscape(errorMsg) + "\")")
            else
              action.respondText(errorMsg)
          }

          AccessLog.logDynamicContentAccess(action, beginTimestamp, 0, false, e)
        }
    }
  }

  //----------------------------------------------------------------------------

  private def newActionActorRef(actionClass: Class[_ <: Action]): (Action, ActorRef) = {
    // Use ReflectASM, which is included by Twitter Chill
    // https://code.google.com/p/reflectasm/
    var action: Action = null
    val actorRef = Config.actorSystem.actorOf(Props {
      action = ConstructorAccess.get(actionClass).newInstance()
      action
    })
    (action, actorRef)
  }

  /** @return true if the cache was hit */
  private def tryCache(action: Action)(f: => Unit): Boolean = {
    ResponseCacher.getCachedResponse(action) match {
      case None =>
        f
        false

      case Some(response) =>
        action.channel.write(response)
        true
    }
  }

  private def runAroundAndAfterFilters(action: Action) {
    action.callAroundFilters(action)
    action.callAfterFilters()
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
      case Some((actionClass, pathParams)) =>
        env.pathParams = pathParams
        dispatchWithFailsafe(actionClass, env)

      case None =>
        Routes.error404 match {
          case None =>
            val response = new DefaultHttpResponse(HTTP_1_1, NOT_FOUND)
            XSendFile.set404Page(response, false)
            env.response = response
            ctx.getChannel.write(env)

          case Some(actionClass) =>
            env.pathParams = MMap.empty
            env.response.setStatus(NOT_FOUND)
            dispatchWithFailsafe(actionClass, env)
        }
    }
  }
}
