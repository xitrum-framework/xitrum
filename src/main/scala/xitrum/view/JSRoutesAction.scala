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
import xitrum.util.Gzip

object JSRoutesAction {
  private var gzipped: Array[Byte] = null

  def jsRoutes(action: Action): String = {
    "var XITRUM_BASE_URI = '" + Config.baseUri + "';\n" +
    "var XITRUM_ROUTES = " + Routes.jsRoutes + ";\n" +
    "var XITRUM_COMET_GET_ACTION = '" + action.urlForPostback[CometGetAction] + "';"
  }

  def gzippedJsRoutes(action: Action): Array[Byte] = synchronized {
    if (gzipped == null) {
      val js  = jsRoutes(action)
      gzipped = Gzip.compress(js.getBytes(Config.paramCharsetName))
    }
    gzipped
  }
}

@GET("/xitrum/routes.js")
class JSRoutesAction extends Action {
  override def execute {
    // See PublicResourceServerAction
    val etag = Etag.forBytes(Routes.jsRoutes.getBytes)
    if (!Etag.respondIfEtagsMatch(this, etag)) {
      response.setHeader(ETAG, etag)
      NotModified.setMaxAgeUntilNextServerRestart(response)

      if (Config.isProductionMode) {
        jsRender(JSRoutesAction.jsRoutes(this))
      } else {
        response.setHeader(CONTENT_TYPE, "text/javascript")
        response.setHeader(CONTENT_ENCODING, "gzip")
        renderBinary(JSRoutesAction.gzippedJsRoutes(this))
      }
    }
  }
}
