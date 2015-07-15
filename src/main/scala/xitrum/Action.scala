package xitrum

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.Duration
import scala.util.control.NonFatal

import io.netty.channel.{ChannelFuture, ChannelFutureListener}
import io.netty.handler.codec.http.{HttpMethod, HttpResponseStatus}

import org.apache.commons.lang3.exception.ExceptionUtils

import xitrum.action._
import xitrum.exception.{InvalidAntiCsrfToken, InvalidInput, MissingParam}
import xitrum.handler.{AccessLog, NoRealPipelining}
import xitrum.handler.inbound.Dispatcher
import xitrum.handler.outbound.ResponseCacher
import xitrum.scope.request.RequestEnv
import xitrum.scope.session.{Csrf, SessionEnv}
import xitrum.view.{Renderer, Responder}

object Action {
  val TIMEOUT = Duration(5, TimeUnit.SECONDS)
}

/**
 * When there's a request comes in, action extending Action will be run directly
 * on the current Netty IO thread. This gives maximum speed when the action is
 * simple and nonblocking.
 *
 * See also FutureAction and ActorAction.
 */
trait Action extends RequestEnv
  with SessionEnv
  with Log
  with Net
  with Filter
  with BasicAuth
  with Redirect
  with Url
  with Renderer
  with Responder
  with I18n
{
  /** This is convenient, for example, when you want to get the current action in view templates. */
  implicit val currentAction = Action.this

  /** This is convenient, for example, when you use scala.concurrent.Future. */
  implicit val executionContext = Config.actorSystem.dispatcher

  /** This is convenient, for example, when you use scala.concurrent.Await. */
  implicit val timeout = Action.TIMEOUT

  //----------------------------------------------------------------------------

  /**
   * Called when the HTTP request comes in.
   * Actions have to implement this method.
   */
  def execute()

  def addConnectionClosedListener(listener: => Unit) {
    channel.closeFuture.addListener(new ChannelFutureListener {
      def operationComplete(future: ChannelFuture) { listener }
    })
  }

  def newComponent[T <: Component : Manifest](): T = {
    val componentClass = manifest[T].runtimeClass.asInstanceOf[Class[Component]]
    val component      = Dispatcher.newAction(componentClass).asInstanceOf[T]
    component.apply(this)
    component
  }

  //----------------------------------------------------------------------------

  def dispatchWithFailsafe() {
    val beginTimestamp = System.currentTimeMillis()
    val route          = handlerEnv.route
    val cacheSecs      = if (route == null) 0 else route.cacheSecs
    var hit            = false

    try {
      if ((request.getMethod == HttpMethod.POST ||
           request.getMethod == HttpMethod.PUT ||
           request.getMethod == HttpMethod.PATCH ||
           request.getMethod == HttpMethod.DELETE) &&
          !isInstanceOf[SkipCsrfCheck] &&
          !Csrf.isValidToken(this)) throw new InvalidAntiCsrfToken

      // Before filters:
      // When not passed, the before filters must explicitly respond to client,
      // with appropriate response status code, error description etc.
      // This logic is app-specific, Xitrum cannot does it for the app.

      if (cacheSecs > 0) {     // Page cache
        hit = tryCache {
          val passed = callBeforeFilters()
          if (passed) callExecuteWrappedInAroundFiltersThenAfterFilters()
        }
      } else {
        val passed = callBeforeFilters()
        if (passed) {
          if (cacheSecs < 0)  // Action cache
            hit = tryCache { callExecuteWrappedInAroundFiltersThenAfterFilters() }
          else                // No cache
            callExecuteWrappedInAroundFiltersThenAfterFilters()
        }
      }

      if (!forwarding) AccessLog.logActionAccess(this, beginTimestamp, cacheSecs, hit)
    } catch {
      case NonFatal(e) if forwarding =>
        log.warn("Error", e)

      case NonFatal(e) if e.isInstanceOf[InvalidAntiCsrfToken] || e.isInstanceOf[MissingParam] || e.isInstanceOf[InvalidInput] =>
        // These exceptions are special cases:
        // We know that the exception is caused by the client (bad request)
        val msg = if (e.isInstanceOf[InvalidAntiCsrfToken]) {
          session.clear()
          "Session expired. Please refresh your browser."
        } else if (e.isInstanceOf[MissingParam]) {
          val mp  = e.asInstanceOf[MissingParam]
          "Missing param: " + mp.key
        } else {
          val ve = e.asInstanceOf[InvalidInput]
          "Validation error: " + ve.message
        }

        response.setStatus(HttpResponseStatus.BAD_REQUEST)
        if (isAjax)
          jsRespond("alert(\"" + jsEscape(msg) + "\")")
        else
          respondText(msg)

        AccessLog.logActionAccess(this, beginTimestamp, 0, false)

      case NonFatal(e) =>
        if (!isDoneResponding) {
          response.setStatus(HttpResponseStatus.INTERNAL_SERVER_ERROR)
          if (Config.productionMode) {
            Config.routes.error500 match {
              case None =>
                respondDefault500Page()

              case Some(error500) =>
                if (error500 == getClass) {
                  respondDefault500Page()
                } else {
                  response.setStatus(HttpResponseStatus.INTERNAL_SERVER_ERROR)
                  Dispatcher.dispatch(error500, handlerEnv)
                }
            }
          } else {
            val errorMsg = e.toString + "\n\n" + ExceptionUtils.getStackTrace(e)
            if (isAjax)
              jsRespond("alert(\"" + jsEscape(errorMsg) + "\")")
            else
              respondText(errorMsg)
          }
        }
        AccessLog.logActionAccess(this, beginTimestamp, 0, false, e)
    }
  }

  /** @return true if the cache was hit */
  private def tryCache(f: => Unit): Boolean = {
    ResponseCacher.getCachedResponse(handlerEnv) match {
      case None =>
        f  // Execute f
        false

      case Some(response) =>
        val future = channel.writeAndFlush(response)
        NoRealPipelining.if_keepAliveRequest_then_resumeReading_else_closeOnComplete(request, channel, future)
        handlerEnv.release()
        true
    }
  }

  private def callExecuteWrappedInAroundFiltersThenAfterFilters() {
    callExecuteWrappedInAroundFilters()
    callAfterFilters()
  }
}
