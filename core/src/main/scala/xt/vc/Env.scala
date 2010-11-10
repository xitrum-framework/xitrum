package xt.vc

import xt.Config
import xt.vc.session._
import xt.vc.env._
import xt.vc.helper._

import java.util.{Map => JMap, List => JList, Set => JSet}

import org.jboss.netty.channel.ChannelHandlerContext
import org.jboss.netty.handler.codec.http.{HttpRequest, HttpResponse, HttpMethod, DefaultCookie}

/** All state variables for a request are put here */
class Env {
  def apply(other: Env) {
    request = other.request
    response = other.response
    method = other.method
    pathInfo = other.pathInfo
    lastUpstreamHandlerCtx = other.lastUpstreamHandlerCtx
    allParams = other.allParams
  }

  var request:  HttpRequest  = _
  var response: HttpResponse = _

  var method: HttpMethod = _

  var uri: String = _

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

  lazy val cookies = CookieDecoder.decode(this)

  lazy val session = {
    val ret = SessionRestorer.restore(this)

    // Store session ID to cookie
    SessionUtil.findSessionCookie(cookies) match {
      case Some(cookie) =>
        cookie.setHttpOnly(true)
        cookie.setPath("/")
        cookie.setValue(ret.id)

      case None =>
        val cookie = new DefaultCookie(Config.sessionIdName, ret.id)
        cookie.setHttpOnly(true)
        cookie.setPath("/")
        cookies.add(cookie)
    }

    ret
  }

  lazy val at = new At
}
