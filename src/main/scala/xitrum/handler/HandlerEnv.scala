package xitrum.handler

import scala.collection.mutable.{HashMap => MHashMap, Map => MMap}

import org.jboss.netty.channel.Channel
import org.jboss.netty.handler.codec.http.{HttpRequest, HttpResponse}
import org.jboss.netty.handler.codec.http.multipart.HttpPostRequestDecoder

import xitrum.Action
import xitrum.routing.Route
import xitrum.scope.request.{FileUploadParams, Params, PathInfo}

/**
 * Env is basically a map for sharing data between handlers. But for more
 * typesafe, fixed data variables are put inside.
 */
class HandlerEnv extends MHashMap[Any, Any] {
  // Set by BodyParser
  var channel:        Channel                = _
  var bodyDecoder:    HttpPostRequestDecoder = _  // Used to clean upload files when connection is closed or response is sent
  var bodyTextParams: Params                 = _
  var bodyFileParams: FileUploadParams       = _  // The filename has been sanitized for insecure character
  var request:        HttpRequest            = _
  var response:       HttpResponse           = _

  // Set by UriParser
  var pathInfo:    PathInfo = _
  var queryParams: Params   = _

  // Set by Dispatcher
  var route:      Route  = _  // The matched route
  var pathParams: Params = _  // The above params are real from the request, this one is logical from the request URL

  /**
   * The merge of all text params (queryParams, bodyParams, and pathParams),
   * as contrast to file upload (fileParams).
   *
   * A val not a def, for speed, so that the calculation is done only once.
   *
   * lazy, so that bodyParams can be changed by ValidatorCaller.
   * Because this is a val, once this is accessed, either of the 3 params should
   * not be changed, because the change will not be reflected. If you still want
   * to change the the 3 params, after changing them, please also change this
   * textParams.
   */
  lazy val textParams: Params = {
    val ret = MMap.empty[String, Seq[String]]

    // The order is important because we want the later to overwrite the former
    ret ++= queryParams
    ret ++= bodyTextParams
    ret ++= pathParams

    ret
  }

  /** The merge of queryParams and pathParams, things that appear in the request URL. */
  lazy val urlParams: Params = {
    val ret = MMap.empty[String, Seq[String]]

    // The order is important because we want the later to overwrite the former
    ret ++= queryParams
    ret ++= pathParams

    ret
  }
}
