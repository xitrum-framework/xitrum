package xitrum.view

import io.netty.handler.codec.http.{HttpHeaders, HttpResponseStatus}
import HttpHeaders.Names._
import HttpHeaders.Values._
import HttpResponseStatus._

import xitrum.{Action, Config}
import xitrum.annotation.GET
import xitrum.comet.CometGetAction
import xitrum.etag.{Etag, NotModified}
import xitrum.routing.Routes
import xitrum.util.Gzip

object JSRoutesAction {
  private var js:        String      = null
  private var gzippedJs: Array[Byte] = null

  def jsRoutes(action: Action): String = synchronized {
    if (js == null) {
      js =
        "var XITRUM_BASE_URI = '" + Config.baseUri + "';\n" +
        "var XITRUM_ROUTES = " + Routes.jsRoutes + ";\n" +
        "var XITRUM_COMET_GET_ACTION = '" + action.urlForPostback[CometGetAction] + "';"
    }
    js
  }

  def gzippedJsRoutes(action: Action): Array[Byte] = synchronized {
    if (gzippedJs == null) {
      gzippedJs = Gzip.compress(jsRoutes(action).getBytes(Config.config.request.charset))
    }
    gzippedJs
  }

  // This value is stable, even across different servers in a cluster
  lazy val etag = Etag.forString(Routes.jsRoutes)
}

@GET("/xitrum/routes.js")
class JSRoutesAction extends Action {
  override def execute {
    if (!Etag.respondIfEtagsIdentical(this, JSRoutesAction.etag)) {
      NotModified.setClientCacheAggressively(response)
      response.setHeader(CONTENT_TYPE, "text/javascript")
      if (Gzip.isAccepted(request)) {
        response.setHeader(CONTENT_ENCODING, "gzip")
        renderBinary(JSRoutesAction.gzippedJsRoutes(this))
      } else {
        jsRender(JSRoutesAction.jsRoutes(this))
      }
    }
  }
}
