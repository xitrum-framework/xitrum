package xitrum.controller

import java.lang.reflect.Method
import org.jboss.netty.handler.codec.http.HttpMethod

import xitrum.Controller
import xitrum.routing.{HttpMethodSockJS, HttpMethodWebSocket, Route, RouteCompiler, RouteOrder, Routes, RouteToken}

/**
 * val action1 = GET("pattern") {
 * }
 *
 * val action2 = GET {
 * }
 *
 * val action3 = first.cacheActionSecond(30).GET("pattern") {
 * }
 *
 * val action4 = cacheActionSecond(30).first.GET("pattern") {
 * }
 *
 * val action5 = first.GET("pattern") {
 * }
 *
 * val action6 = cacheActionSecond(30).GET("pattern") {
 * }
 *
 * See Action, methods here relates to those there.
 */
trait ActionFactory {
  this: Controller =>

  // Curry causes problem: ambiguous reference to overloaded definition
  // http://www.scala-lang.org/node/1360

  //----------------------------------------------------------------------------

  /** Creates route for 404 or 500 error handler. */
  def errorAction(body: => Unit) = Action(null, null, () => body, 0)

  //----------------------------------------------------------------------------

  // first and last should be "lazy val" or "def" so that they are run after
  // pathPrefix has been set

  lazy val first = Action(Route(null, RouteOrder.FIRST, PathPrefix.toCompiledPattern(pathPrefix)), null, null, 0)
  lazy val last  = Action(Route(null, RouteOrder.LAST,  PathPrefix.toCompiledPattern(pathPrefix)), null, null, 0)

  def cacheActionSecond(seconds: Int) = Action(Route(null, RouteOrder.OTHER, PathPrefix.toCompiledPattern(pathPrefix)), null, null, -seconds)
  def cacheActionMinute(minutes: Int) = Action(Route(null, RouteOrder.OTHER, PathPrefix.toCompiledPattern(pathPrefix)), null, null, -minutes * 60)
  def cacheActionHour  (hours:   Int) = Action(Route(null, RouteOrder.OTHER, PathPrefix.toCompiledPattern(pathPrefix)), null, null, -hours * 60 * 60)
  def cacheActionDay   (days:    Int) = Action(Route(null, RouteOrder.OTHER, PathPrefix.toCompiledPattern(pathPrefix)), null, null, -days * 60 * 60 * 24)

  def cachePageSecond(seconds: Int) = Action(Route(null, RouteOrder.OTHER, PathPrefix.toCompiledPattern(pathPrefix)), null, null, seconds)
  def cachePageMinute(minutes: Int) = Action(Route(null, RouteOrder.OTHER, PathPrefix.toCompiledPattern(pathPrefix)), null, null, minutes * 60)
  def cachePageHour  (hours:   Int) = Action(Route(null, RouteOrder.OTHER, PathPrefix.toCompiledPattern(pathPrefix)), null, null, hours * 60 * 60)
  def cachePageDay   (days:    Int) = Action(Route(null, RouteOrder.OTHER, PathPrefix.toCompiledPattern(pathPrefix)), null, null, days * 60 * 60 * 24)

  //----------------------------------------------------------------------------

  private def withPathPrefix(pattern: String) = {
    if (pathPrefix.isEmpty) pattern else pathPrefix + "/" + pattern
  }

  def GET(pattern: String)(body: => Any) =
    Action(Route(HttpMethod.GET, RouteOrder.OTHER, RouteCompiler.compile(withPathPrefix(pattern))), null, () => body, 0)

  def GET(body: => Any) =
    Action(Route(HttpMethod.GET, RouteOrder.OTHER, RouteCompiler.compile(withPathPrefix(""))), null, () => body, 0)

  def POST(pattern: String)(body: => Any) =
    Action(Route(HttpMethod.POST, RouteOrder.OTHER, RouteCompiler.compile(withPathPrefix(pattern))), null, () => body, 0)

  def POST(body: => Any) =
    Action(Route(HttpMethod.POST, RouteOrder.OTHER, RouteCompiler.compile(withPathPrefix(""))), null, () => body, 0)

  def PUT(pattern: String)(body: => Any) =
    Action(Route(HttpMethod.PUT, RouteOrder.OTHER, RouteCompiler.compile(withPathPrefix(pattern))), null, () => body, 0)

  def PUT(body: => Any) =
    Action(Route(HttpMethod.PUT, RouteOrder.OTHER, RouteCompiler.compile(withPathPrefix(""))), null, () => body, 0)

  def PATCH(pattern: String)(body: => Any) =
    Action(Route(HttpMethod.PATCH, RouteOrder.OTHER, RouteCompiler.compile(withPathPrefix(pattern))), null, () => body, 0)

  def PATCH(body: => Any) =
    Action(Route(HttpMethod.PATCH, RouteOrder.OTHER, RouteCompiler.compile(withPathPrefix(""))), null, () => body, 0)

  def DELETE(pattern: String)(body: => Any) =
    Action(Route(HttpMethod.DELETE, RouteOrder.OTHER, RouteCompiler.compile(withPathPrefix(pattern))), null, () => body, 0)

  def DELETE(body: => Any) =
    Action(Route(HttpMethod.DELETE, RouteOrder.OTHER, RouteCompiler.compile(withPathPrefix(""))), null, () => body, 0)

  def OPTIONS(pattern: String)(body: => Any) =
    Action(Route(HttpMethod.OPTIONS, RouteOrder.OTHER, RouteCompiler.compile(withPathPrefix(pattern))), null, () => body, 0)

  def OPTIONS(body: => Any) =
    Action(Route(HttpMethod.OPTIONS, RouteOrder.OTHER, RouteCompiler.compile(withPathPrefix(""))), null, () => body, 0)

  def WEBSOCKET(pattern: String)(body: => Any) =
    Action(Route(HttpMethodWebSocket, RouteOrder.OTHER, RouteCompiler.compile(withPathPrefix(pattern))), null, () => body, 0)

  def WEBSOCKET(body: => Any) =
    Action(Route(HttpMethodWebSocket, RouteOrder.OTHER, RouteCompiler.compile(withPathPrefix(""))), null, () => body, 0)

  def SOCKJS(pattern: String)(body: => Any) =
    Action(Route(HttpMethodSockJS, RouteOrder.OTHER, RouteCompiler.compile(withPathPrefix(pattern))), null, () => body, 0)

  def SOCKJS(body: => Any) =
    Action(Route(HttpMethodSockJS, RouteOrder.OTHER, RouteCompiler.compile(withPathPrefix(""))), null, () => body, 0)
}

object PathPrefix {
  // Used by ActionFactory
  def toCompiledPattern(pathPrefix: String) = Seq(RouteToken(pathPrefix, true, None))

  // Used by Action
  def fromCompiledPattern(routeToken: Seq[RouteToken]) = routeToken.head.value
}
