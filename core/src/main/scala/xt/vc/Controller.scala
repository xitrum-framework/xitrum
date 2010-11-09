package xt.vc

import xt.vc.controller._

import org.jboss.netty.handler.codec.http._
import HttpHeaders.Names._
import HttpResponseStatus._

trait Controller extends Helper with Filter with Renderer {
  def respond {
    lastUpstreamHandlerCtx.getChannel.write(this)
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

    response.setHeader(LOCATION, location2)
    respond
  }
}
