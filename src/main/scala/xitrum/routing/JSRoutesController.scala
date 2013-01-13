package xitrum.routing

import org.jboss.netty.handler.codec.http.{HttpHeaders, HttpResponseStatus}
import HttpHeaders.Names._
import HttpHeaders.Values._
import HttpResponseStatus._

import xitrum.{Controller, Config}
import xitrum.etag.{Etag, NotModified}
import xitrum.util.Gzip

object JSRoutesCache {
  // This value is stable, even across different servers in a cluster
  lazy val etag = Etag.forString(Routes.jsRoutes)

  lazy val routes =
    "var XITRUM_BASE_URL = '" + Config.baseUrl  + "';\n" +
    "var XITRUM_ROUTES   = "  + Routes.jsRoutes + ";\n"

  lazy val gzippedRoutes =
    Gzip.compress(routes.getBytes(Config.xitrum.request.charset))
}

object JSRoutesController extends JSRoutesController

class JSRoutesController extends Controller {
  def routes = GET("xitrum/routes.js") {
    if (!Etag.respondIfEtagsIdentical(this, JSRoutesCache.etag)) {
      NotModified.setClientCacheAggressively(response)
      response.setHeader(CONTENT_TYPE, "text/javascript")
      if (Gzip.isAccepted(request)) {
        response.setHeader(CONTENT_ENCODING, "gzip")
        respondBinary(JSRoutesCache.gzippedRoutes)
      } else {
        jsRespond(JSRoutesCache.routes)
      }
    }
  }
}
