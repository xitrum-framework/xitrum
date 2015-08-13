package xitrum.handler

import scala.collection.mutable.{HashMap => MHashMap, Map => MMap}

import io.netty.channel.Channel
import io.netty.handler.codec.http.{FullHttpRequest, FullHttpResponse}
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder

import xitrum.Config
import xitrum.routing.Route
import xitrum.scope.request.{FileUploadParams, Params, PathInfo}

import org.json4s.{JObject, JValue}
import org.json4s.jackson.JsonMethods

/**
 * Env is basically a map for sharing data between handlers. But for more
 * typesafe, fixed data variables are put inside.
 */
class HandlerEnv extends MHashMap[Any, Any] {
  // Set by Request2Env
  var channel:        Channel                = _
  /** Used to clean upload files when connection is closed or response is sent. */
  var bodyDecoder:    HttpPostRequestDecoder = _
  var bodyTextParams: Params                 = _
  /** The filename has been sanitized for insecure characters. */
  var bodyFileParams: FileUploadParams       = _
  var request:        FullHttpRequest        = _
  var response:       FullHttpResponse       = _

  // Set by UriParser
  /** Part of the URL before the query part. Ex: When URL is /search?q=xitrum, pathInfo is "/search" */
  var pathInfo:    PathInfo = _
  /** Params after the question mark of the URL. Ex: /search?q=xitrum */
  var queryParams: Params   = _

  // Set by Dispatcher
  /** The matched route. */
  var route:      Route  = _
  /** Params embedded in the path. Ex: /articles/:id */
  var pathParams: Params = _

  /** The merge of queryParams and pathParams, things that appear in the request URL. */
  lazy val urlParams: Params = {
    val ret = MMap.empty[String, Seq[String]]

    // The order is important because we want the later to overwrite the former
    ret ++= queryParams
    ret ++= pathParams

    ret
  }

  /**
   * The merge of all text params (queryParams, bodyParams, and pathParams),
   * as contrast to file upload (bodyFileParams).
   */
  lazy val textParams: Params = {
    // A val not a def, for speed, so that the calculation is done only once.
    //
    // lazy, so that bodyParams can be changed by ValidatorCaller.
    // Because this is a val, once this is accessed, either of the 3 params should
    // not be changed, because the change will not be reflected. If you still want
    // to change the the 3 params, after changing them, please also change this
    // textParams.

    val ret = MMap.empty[String, Seq[String]]

    // The order is important because we want the later to overwrite the former
    ret ++= queryParams
    ret ++= bodyTextParams
    ret ++= pathParams

    ret
  }

  /** The whole request body as String. */
  lazy val requestContentString: String = {
    val byteBuf = request.content
    byteBuf.toString(Config.xitrum.request.charset)
  }

  /** The whole request body parsed as JSON4S JValue. */
  lazy val requestContentJValue: JValue = if (requestContentString.isEmpty) {
    JObject()
  } else {
    JsonMethods.parseOpt(requestContentString).getOrElse(JObject())
  }

  /** Releases native memory used by the request, response, and bodyDecoder. */
  def release() {
    if (request .refCnt() > 0) request .release()
    if (response.refCnt() > 0) response.release()

    if (bodyDecoder != null) {
      bodyDecoder.cleanFiles()
      bodyDecoder.destroy()
      bodyDecoder = null
    }
  }
}
