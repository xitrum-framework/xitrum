package xitrum.handler

import scala.collection.mutable.HashMap
import org.jboss.netty.handler.codec.http.{HttpRequest, HttpResponse}

import xitrum.Action
import xitrum.scope.{Env => CEnv, PathInfo}

/**
 * Env is basically a map for sharing data between handlers. But for more
 * typesafe, fixed data variables are put inside.
 */
class Env extends HashMap[String, Any] {
  import CEnv._

  var request:          HttpRequest      = null  // Set by Request2Env
  var response:         HttpResponse     = null  // Set before the response is sent to client

  var pathInfo:         PathInfo         = null  // Set by UriParser
  var uriParams:        Params           = null

  var bodyParams:       Params           = null  // Set by BodyParser
  var fileUploadParams: FileUploadParams = null  // Set by BodyParser

  var action:           Action           = null  // Set by Dispatcher's dispatchWithFailsafe
  var pathParams:       Params           = null  // Set by Dispatcher
}
