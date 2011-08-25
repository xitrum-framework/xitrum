package xitrum.view

import xitrum.{Action, Config}
import xitrum.annotation.GET
import xitrum.comet.CometGetAction
import xitrum.routing.Routes

@GET("/xitrum/routes.js")
class JSRoutesAction extends Action {
  override def execute {
    jsRender(
      "var XITRUM_BASE_URI = '" + Config.baseUri + "'",
      "var XITRUM_ROUTES = " + Routes.jsRoutes,
      "var XITRUM_COMET_GET_ACTION = '" + urlForPostback[CometGetAction] + "'"
    )
  }
}
