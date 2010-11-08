package xt.handler

import xt.vc.session._

import java.util.{Map => JMap, List => JList, Set => JSet}

import org.jboss.netty.handler.codec.http.{HttpRequest, HttpResponse, HttpMethod, Cookie => NCookie}

class XtEnv {
  var request:  HttpRequest  = _
  var response: HttpResponse = _

  /**
   * controller and action are put here.
   *
   * Design decision: Jav Map is used instead of Scala Map because Netty produces
   * JMap and we want to avoid costly conversion from Java Map to Scala Map.
   */
  var params: JMap[String, JList[String]] = _

  /**
   * pathInfo for http://example.com/articles?page=2 is /articles
   */
  var pathInfo: String = _

  var method: HttpMethod = _

  var cookies: JSet[NCookie] = _
  var session: Session       = _
}
