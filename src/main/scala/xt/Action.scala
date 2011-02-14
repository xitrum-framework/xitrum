package xt

import org.jboss.netty.handler.codec.http._
import HttpHeaders.Names._
import HttpResponseStatus._

import xt.vc.action._
import xt.vc.env.ExtEnv
import xt.vc.view.{JQuery, Renderer}

trait Action extends ExtEnv with Logger with Net with ParamAccess with Filter with Flash with BasicAuthentication with CSRF with Renderer with JQuery {
  def execute

  //----------------------------------------------------------------------------

  // FIXME: this causes warning
  // "the initialization is no longer be executed before the superclass is called"

  private var _responded = false

  def respond = synchronized {
    if (_responded) {
      // Print the stack trace so that application developers know where to fix
      try {
        throw new Exception
      } catch {
        case e => logger.warn("Double respond", e)
      }
    } else {
      _responded = true

      clearFlashWhenRespond
      cookies.setCookiesWhenRespond(this)

      henv("response") = response
      ctx.getChannel.write(henv)
    }
  }

  // Called by Dispatcher
  def responded = _responded

  //----------------------------------------------------------------------------

  def urlFor[T: Manifest](params: (String, Any)*) = {
    val actionClass = manifest[T].erasure.asInstanceOf[Class[Action]]
    xt.routing.Routes.urlFor(actionClass, params:_*)
  }

  /**
   * When there are no params, the programmer can write urlFor[MyAction],
   * instead of urlFor[MyAction]().
   */
  def urlFor[T: Manifest]: String = urlFor[T]()

  //----------------------------------------------------------------------------

  def redirectTo(location: String, status: HttpResponseStatus = FOUND) {
    response.setStatus(status)

    HttpHeaders.setContentLength(response, 0)
    response.setHeader(LOCATION, location)
    respond
  }

  def redirectTo[T: Manifest] { redirectTo(urlFor[T]) }

  def redirectTo[T: Manifest](params: (String, Any)*) { redirectTo(urlFor[T](params:_*)) }
}
