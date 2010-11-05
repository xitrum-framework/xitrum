package xt.middleware

import xt.framework.Controller

import java.lang.reflect.Method
import java.util.{Map => JMap, List => JList, Set => JSet}
import scala.collection.mutable.HashMap

import org.jboss.netty.handler.codec.http.{HttpMethod, Cookie => NCookie}

/**
 * Used by Netty handler, middlewares, and controllers to share data for both
 * request and response pipeline processing.
 *
 * Design decision:
 * See https://github.com/mmcgrana/ring/blob/master/SPEC
 * HashMap[String, Any] is not used directly so that strong type
 * checking can be done in the spirit of Scala.
 */
class Env extends HashMap[String, Any] {
  var serverPort: Int
  var serverName: String
  var remoteAddr: String
  var uri: String
  var queryString: Option[String]
  var scheme: String
  var requestMethod: HttpMethod = _
  var contentType: Option[String]
  var contentLength: Option[Long]
  var characterEncoding: Option[String]

  var pathInfo: String = _

  var cookies: JSet[NCookie] = _
  var session: Session       = _

  /**
   * controller and action are put here.
   *
   * Design decision: Jav Map is used instead of Scala Map because Netty produces
   * JMap and we want to avoid costly conversion from Java Map to Scala Map.
   */
  var params: JMap[String, JList[String]] = _

  var controller: Controller = _
  var action:     Method     = _

  /**
   * By default the Netty handler will send the response to the client. This
   * operataion will end the HTTP request. If you want to send the response
   * yourself, set this to false.
   */
  var autoRespond = true
}
