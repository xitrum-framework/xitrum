package xitrum.handler

import scala.collection.mutable.HashMap
import org.jboss.netty.handler.codec.http.{HttpRequest, HttpResponse}
import xitrum.action.env.{Env => CEnv, PathInfo}

/**
 * Env is basically a map for sharing data between handlers. But for more
 * type-safe, fixed data variables are put inside.
 */
class Env extends HashMap[String, Any] {
  // FIXME: calculate when SSL handling feature is added to Xitrum.
  /**
   * Xitrum is not only a web framework, but also a web server. This value is
   * true if the request Xitrum web server handles is HTTPS.
   *
   * If the original request is HTTPS, but has been handled by load balancers or
   * reverse proxy servers like Nginx, this value is still false.
   */
  var ssl:        Boolean      = false

  var request:    HttpRequest  = null  // Set by Request2Env
  var response:   HttpResponse = null  // Set before the response is sent to client

  var pathInfo:   PathInfo     = null  // Set by UriParser
  var uriParams:  CEnv.Params  = null

  var bodyParams: CEnv.Params  = null  // Set by BodyParser
  var pathParams: CEnv.Params  = null  // Set by Dispatcher
}
