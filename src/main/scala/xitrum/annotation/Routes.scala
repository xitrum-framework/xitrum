package xitrum.annotation

import scala.annotation.StaticAnnotation

sealed trait Route        extends StaticAnnotation
sealed trait Error        extends StaticAnnotation
sealed trait RouteOrder   extends StaticAnnotation

case class GET   (paths: String*) extends Route
case class POST  (paths: String*) extends Route
case class PUT   (paths: String*) extends Route
case class PATCH (paths: String*) extends Route
case class DELETE(paths: String*) extends Route

case class WEBSOCKET(paths: String*) extends Route
case class SOCKJS   (paths: String*) extends Route

class First extends RouteOrder
class Last  extends RouteOrder

/**
 * If there's no SockJsCookieNeeded or SockJsNoCookieNeeded annotation, the cookie
 * config is taken from sockJsCookie in xitrum.conf.
 */
class SockJsCookieNeeded extends StaticAnnotation

/** See SockJsCookieNeeded. */
class SockJsNoCookieNeeded extends StaticAnnotation

/** WebSocket is always turn on for SockJS. To turn off, use this annotation. */
class SockJsNoWebSocket extends StaticAnnotation

class Error404 extends Error
class Error500 extends Error
