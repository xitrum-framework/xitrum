package xitrum.action.env

import java.util.{Map => JMap, List => JList}

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

  def apply(ctx: ChannelHandlerContext, henv: HEnv) {
    this.ctx        = ctx
    this.henv       = henv

    this.request    = henv.request
    this.pathInfo   = henv.pathInfo
    this.uriParams  = henv.uriParams
    this.bodyParams = henv.bodyParams
    this.fileParams = henv.fileParams
    this.pathParams = henv.pathParams
  }
}
