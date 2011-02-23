package xitrum.action.env

import java.util.{Map => JMap, List => JList}

import org.jboss.netty.channel.ChannelHandlerContext
import org.jboss.netty.handler.codec.http.HttpRequest

import xitrum.handler.{Env => HEnv}

object Env {
  /**
   * Design decision: Java Map is used instead of Scala Map because Netty's
   * QueryStringDecoder#getParameters produces Java Map[String, java.util.List[String]]
   * and we want to avoid costly conversion from Java Map to Scala Map.
   */
  type Params = JMap[String, JList[String]]
}

/**
 * All core state variables for a request are here. All other variables in Helper
 * and Controller can be inferred from these variables.
 */
class Env {
  var ctx:        ChannelHandlerContext = _
  var henv:       HEnv                  = _

  // Shortcuts from henv for easy access for web developers
  var request:    HttpRequest           = _
  var pathInfo:   PathInfo              = _
  var uriParams:  Env.Params            = _
  var bodyParams: Env.Params            = _
  var pathParams: Env.Params            = _

  def apply(ctx: ChannelHandlerContext, henv: HEnv) {
    this.ctx        = ctx
    this.henv       = henv

    this.request    = henv.request
    this.pathInfo   = henv.pathInfo
    this.uriParams  = henv.uriParams
    this.bodyParams = henv.bodyParams
    this.pathParams = henv.pathParams
  }
}
