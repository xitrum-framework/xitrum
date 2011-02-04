package xt

import org.jboss.netty.handler.codec.http._
import HttpHeaders.Names._
import HttpResponseStatus._

import xt.vc.controller._
import xt.vc.env.ExtendedEnv
import xt.vc.view.Renderer

trait Controller extends ExtendedEnv with ServletLike with Logger with Net with ParamAccess with Url with Filter with BasicAuthentication with Renderer {
  // async and responded are not mutually exclusive, see respond and respondLater.

  // FIXME: this causes warning
  // "the initialization is no longer be executed before the superclass is called"

  private var async     = false
  private var responded = false

  /**
   * See respondLater. In sync mode, this method will be called by Xitrum if it
   * is not called during the invocation of the controller action.
   */
  def respond = synchronized {
    if (responded) {
      // Print the stack trace so that application developers know where to fix
      try {
        throw new Exception
      } catch {
        case e => logger.warn("Double respond", e)
      }
    } else {
      responded = true
      encodeCookies
      henv("response") = response
      ctx.getChannel.write(henv)
    }
  }

  /**
   * Because normally most of a web application is in sync mode, by default
   * Xitrum is in sync mode, e.g. the response is sent to the client immediately
   * right after the controller action is invoked.
   *
   * To swich to async mode:
   * 1. Call this method.
   * 2. Call respond when the application wants to repond.
   */
  def respondLater {
    if (async) {
      // Print the stack trace so that application developers know where to fix
      try {
        throw new Exception
      } catch {
        case e => logger.warn("Already in async mode", e)
      }
    } else if (responded) {
      try {
        throw new Exception
      } catch {
        case e => logger.warn("Cannot swich to async mode because the response has been sent", e)
      }
    } else {
      async = true
    }
  }

  /**
   * Called by Xitrum to check if it needs to send the response (see ExtendedEnv)
   * immediately after invoking the controller action.
   */
  def isAutoResponseNeeded = !async && !responded

  /**
   * Returns 302 response.
   *
   * @param location Can be:
   * * absolute:                contains "://"
   * * relative to this domain: starts with "/"
   * * Controller#action:       contains "#"
   * * action:                  otherwise
   */
  def redirectTo(location: String, params: Any*) {
    response.setStatus(FOUND)

    val location2 = if (location.contains("://") || location.startsWith("/"))
      location
    else {
      urlFor(location, params)
    }

    HttpHeaders.setContentLength(response, 0)
    response.setHeader(LOCATION, location2)
    respond
  }

  //----------------------------------------------------------------------------

  private def encodeCookies {
    if (cookies != null && cookies.size > 0) {  // == null: CookieDecoder has not been run
      val encoder = new CookieEncoder(true)
      val iter = cookies.iterator
      while (iter.hasNext) encoder.addCookie(iter.next)
      response.setHeader(SET_COOKIE, encoder.encode)
    }
  }
}
