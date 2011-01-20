package xt

import org.jboss.netty.handler.codec.http._
import HttpHeaders.Names._
import HttpResponseStatus._

import xt.vc.controller._
import xt.vc.env.ExtendedEnv
import xt.vc.view.Renderer

trait Controller extends ExtendedEnv with Logger with Net with ParamAccess with Url with Filter with BasicAuthentication with Renderer {
  // FIXME: this causes warning
  // "the initialization is no longer be executed before the superclass is called"
  private var responded = false

  def respond = synchronized {
    if (responded) {
      // Print the stack trace so that application developers know where to fix
      try {
        throw new Exception("Double respond")
      } catch {
        case e =>
          logger.warn("Double respond", e)
      }
    } else {
      responded = true
      encodeCookies
      henv("response") = response
      ctx.getChannel.write(henv)
    }
  }

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
