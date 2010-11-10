package xt.vc

import java.util.{Map => JMap, List => JList}

import org.jboss.netty.channel.ChannelHandlerContext
import org.jboss.netty.handler.codec.http.HttpRequest

object Env {
  /**
   * Design decision: Java Map is used instead of Scala Map because Netty produces
   * Java Map and we want to avoid costly conversion from Java Map to Scala Map.
   */
  type Params = JMap[String, JList[String]]
}

/**
 * All core state variables for a request are here. All other variables in Helper
 * and Controller can be inferred from these variables.
 */
class Env {
  var ctx:         ChannelHandlerContext = _
  var request:     HttpRequest           = _
  var pathInfo:    String                = _
  var uriParams:   Env.Params            = _
  var bodyParams:  Env.Params            = _
  var routeParams: Env.Params            = _

  def apply(ctx:         ChannelHandlerContext,
            request:     HttpRequest,
            pathInfo:    String,
            uriParams:   Env.Params,
            bodyParams:  Env.Params,
            routeParams: Env.Params) {
    this.ctx         = ctx
    this.request     = request
    this.pathInfo    = pathInfo
    this.uriParams   = uriParams
    this.bodyParams  = bodyParams
    this.routeParams = routeParams
  }
}
