package xitrum.handler

import scala.collection.mutable.{HashMap => MHashMap}

import io.netty.channel.Channel
import io.netty.handler.codec.http.{HttpRequest, HttpResponse}

import xitrum.Controller
import xitrum.controller.Action
import xitrum.scope.request.{FileUploadParams, Params, PathInfo}

/**
 * Env is basically a map for sharing data between handlers. But for more
 * typesafe, fixed data variables are put inside.
 */
class HandlerEnv extends MHashMap[String, Any] {
  var channel:          Channel          = null

  var request:          HttpRequest      = null  // Set by Request2Env
  var response:         HttpResponse     = null  // Set before the response is sent to client

  // Set by UriParser
  var pathInfo:         PathInfo         = null
  var uriParams:        Params           = null

  // Set by BodyParser
  var bodyParams:       Params           = null
  var fileUploadParams: FileUploadParams = null  // The filename has been sanitized for insecure character

  // Set by Dispatcher
  var action:           Action           = null
  var pathParams:       Params           = null  // The above params are real from the request, this one is logical from the route
  var controller:       Controller       = null
}
