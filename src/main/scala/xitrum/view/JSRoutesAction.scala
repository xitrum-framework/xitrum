package xitrum.view

import org.jboss.netty.handler.codec.http.{HttpHeaders, HttpResponseStatus}
import HttpHeaders.Names._
import HttpHeaders.Values._
import HttpResponseStatus._

import xitrum.{Action, Config}
import xitrum.annotation.GET
import xitrum.comet.CometGetAction
import xitrum.routing.Routes
import xitrum.util.NotModified

@GET("/xitrum/routes.js")
class JSRoutesAction extends Action {
  override def execute {
    // See PublicResourceServerAction

    if (request.getHeader(IF_MODIFIED_SINCE) == NotModified.serverStartupTimestampRfc2822) {
      response.setStatus(NOT_MODIFIED)
      respond
      return
    }

    response.setHeader(LAST_MODIFIED, NotModified.serverStartupTimestampRfc2822)
    response.setHeader(CACHE_CONTROL, MAX_AGE + "=" + NotModified.SECS_IN_A_YEAR)
    jsRender(
      "var XITRUM_BASE_URI = '" + Config.baseUri + "'",
      "var XITRUM_ROUTES = " + Routes.jsRoutes,
      "var XITRUM_COMET_GET_ACTION = '" + urlForPostback[CometGetAction] + "'"
    )
  }
}
