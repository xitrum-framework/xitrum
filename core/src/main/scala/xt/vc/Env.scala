package xt.vc

import xt.vc.session._
import xt.vc.helper._

import java.util.{Map => JMap, List => JList, Set => JSet}

import org.jboss.netty.channel.ChannelHandlerContext
import org.jboss.netty.handler.codec.http.{HttpRequest, HttpResponse, HttpMethod, Cookie => NCookie}

/** All state variables for a request are put here */
class Env {
  def apply(other: Env) {
    request = other.request
    response = other.response
    method = other.method
    pathInfo = other.pathInfo
    lastUpstreamHandlerCtx = other.lastUpstreamHandlerCtx
    allParams = other.allParams
    cookies = other.cookies
    session = other.session
    at = other.at
  }

  var request:  HttpRequest  = _
  var response: HttpResponse = _

  var method: HttpMethod = _

  /** pathInfo for http://example.com/articles?page=2 is /articles */
  var pathInfo: String = _

  var lastUpstreamHandlerCtx: ChannelHandlerContext = _

  // Below are maps at various scopes

  /**
   * controller and action are put here.
   *
   * Design decision: Java Map is used instead of Scala Map because Netty produces
   * Java Map and we want to avoid costly conversion from Java Map to Scala Map.
   */
  var allParams: JMap[String, JList[String]] = _

  var cookies: JSet[NCookie] = _

  var session: Session = _

  var at: At = _
}
