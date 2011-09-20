package xitrum.view

import org.jboss.netty.handler.codec.http.{HttpHeaders, HttpResponseStatus}
import HttpHeaders.Names._
import HttpHeaders.Values._
import HttpResponseStatus._

import xitrum.{Action, Config}
import xitrum.annotation.GET
import xitrum.comet.CometGetAction
import xitrum.etag.{Etag, NotModified}
import xitrum.routing.Routes

@GET("/xitrum/routes.js")
class JSRoutesAction extends Action {
  override def execute {
    // See PublicResourceServerAction
    val etag = Etag.forBytes(Routes.jsRoutes.getBytes)
    if (!Etag.respondIfEtagsMatch(this, etag)) {
      NotModified.setMaxAgeUntilNextServerRestart(response)
      jsRender(
        "var XITRUM_BASE_URI = '" + Config.baseUri + "'",
        "var XITRUM_ROUTES = " + Routes.jsRoutes,
        "var XITRUM_COMET_GET_ACTION = '" + urlForPostback[CometGetAction] + "'"
      )
    }
  }
}
