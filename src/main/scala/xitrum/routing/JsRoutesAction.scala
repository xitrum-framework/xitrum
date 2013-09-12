package xitrum.routing

import org.jboss.netty.handler.codec.http.{HttpHeaders, HttpResponseStatus}
import HttpHeaders.Names._
import HttpHeaders.Values._
import HttpResponseStatus._

import xitrum.{Action, Config}
import xitrum.annotation.{First, GET}
import xitrum.etag.{Etag, NotModified}
import xitrum.util.Gzip

object JsRoutesCache {
  // This value is stable, even across different servers in a cluster
  lazy val etag = Etag.forString(Config.routes.jsRoutes)

  lazy val routes =
    "var XITRUM_BASE_URL = '" + Config.baseUrl  + "';\n" +
    "var XITRUM_ROUTES   = "  + Config.routes.jsRoutes + ";\n"

  lazy val gzippedRoutes =
    Gzip.compress(routes.getBytes(Config.xitrum.request.charset))
}

@First
@GET("xitrum/routes.js")
class JsRoutesAction extends Action {
  def execute() {
    if (!Etag.respondIfEtagsIdentical(this, JsRoutesCache.etag)) {
      NotModified.setClientCacheAggressively(response)
      response.setHeader(CONTENT_TYPE, "text/javascript")
      if (Gzip.isAccepted(request)) {
        response.setHeader(CONTENT_ENCODING, "gzip")
        respondBinary(JsRoutesCache.gzippedRoutes)
      } else {
        jsRespond(JsRoutesCache.routes)
      }
    }
  }
}
