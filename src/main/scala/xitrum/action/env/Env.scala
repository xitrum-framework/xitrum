package xitrum.action.env

import java.util.{Map => JMap, List => JList, LinkedHashMap => JLinkedHashMap}

import org.jboss.netty.channel.ChannelHandlerContext
import org.jboss.netty.handler.codec.http.{FileUpload, HttpRequest}

import xitrum.handler.{Env => HEnv}

object Env {
  /**
   * Design decision: Java Map is used instead of Scala Map because Netty's
   * QueryStringDecoder#getParameters produces Java Map[String, java.util.List[String]]
   * and we want to avoid costly conversion from Java Map to Scala Map.
   */
  type Params = JMap[String, JList[String]]

  type FileParams = JMap[String, JList[FileUpload]]
}

/**
 * All core state variables for a request are here. All other variables in Helper
 * and Controller can be inferred from these variables.
 */
class Env {
  import Env._

  var ctx:        ChannelHandlerContext = _
  var henv:       HEnv                  = _

  // Shortcuts from henv for easy access for web developers

  var request:    HttpRequest = _
  var pathInfo:   PathInfo    = _

  var uriParams:  Params      = _
  var bodyParams: Params      = _
  var fileParams: FileParams  = _
  var pathParams: Params      = _

  /**
   * text (uriParams, bodyParams, pathParams)  vs file upload (fileParams)
   *
   * Lazily initialized, not initialized in "apply" so that bodyParams can be
   * changed by ValidatorCaller. Because this is a lazy val, once this is accessed,
   * the 3 params should not be changed, because the change will not be reflected
   * by this val.
   *
   * Not a function ("def") so that the calculation is done only once.
   */
  lazy val textParams: Params = {
    val ret = new JLinkedHashMap[String, JList[String]]
    // The order is important because we want the later to overwrite the former
    ret.putAll(uriParams)
    ret.putAll(bodyParams)
    ret.putAll(pathParams)
    ret
  }

  def apply(ctx: ChannelHandlerContext, henv: HEnv) {
    this.ctx        = ctx
    this.henv       = henv

    request    = henv.request
    pathInfo   = henv.pathInfo

    uriParams  = henv.uriParams
    bodyParams = henv.bodyParams
    fileParams = henv.fileParams
    pathParams = henv.pathParams
  }
}
