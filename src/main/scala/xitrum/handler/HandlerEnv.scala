package xitrum.handler

import scala.collection.mutable.{HashMap => MHashMap}

import io.netty.channel.Channel
import io.netty.handler.codec.http.{HttpRequest, HttpResponse}

import xitrum.Action
import xitrum.scope.request.{FileUploadParams, Params, PathInfo}

/**
 * Env is basically a map for sharing data between handlers. But for more
 * typesafe, fixed data variables are put inside.
 */
class HandlerEnv extends MHashMap[String, Any] {
  var channel:          Channel          = null

  var request:          HttpRequest      = null  // Set by Request2Env
  var response:         HttpResponse     = null  // Set before the response is sent to client

  var pathInfo:         PathInfo         = null  // Set by UriParser
  var uriParams:        Params           = null  // Set by UriParser

  var bodyParams:       Params           = null  // Set by BodyParser
  var fileUploadParams: FileUploadParams = null  // Set by BodyParser, the filename has been sanitized for insecure character

  var action:           Action           = null  // Set by Dispatcher's dispatchWithFailsafe
  var pathParams:       Params           = null  // Set by Dispatcher, the above 3 are real from the request, this one is logical from the route
}
