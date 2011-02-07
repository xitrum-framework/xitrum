package xt

import org.jboss.netty.handler.codec.http._
import HttpHeaders.Names._
import HttpResponseStatus._

import xt.vc.controller._
import xt.vc.env.ExtendedEnv
import xt.vc.view.Renderer

trait Controller extends ExtendedEnv with ServletLike with Logger with Net with ParamAccess with Url with Filter with BasicAuthentication with Renderer {
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
      encodeCookies
      henv("response") = response
      ctx.getChannel.write(henv)
    }
  }

  // Called by Dispatcher
  def responded = _responded

  /**
   * @param location
   * * absolute:                contains "://"
   * * relative to this domain: starts with "/"
   * * Controller#action:       contains "#"
   * * action:                  otherwise
   */
  def redirectTo(location: String, status: HttpResponseStatus = FOUND, params: Map[String, Any] = Map()) {
    response.setStatus(status)

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
