package xitrum.controller

import io.netty.handler.codec.http.HttpMethod

import xitrum.Controller
import xitrum.routing.{HttpMethodWebSocket, Route, RouteOrder, Routes}

/**
 * val route = GET("pattern") {
 * }
 *
 * val route = GET() {
 * }
 *
 * val route = first.cacheActionSecond(30).GET("pattern") {
 * }
 *
 * val route = cacheActionSecond(30).first.GET("pattern") {
 * }
 *
 * val route = first.GET("pattern") {
 * }
 *
 * val route = cacheActionSecond(30).GET("pattern") {
 * }
 *
 * See Route, methods here relates to those there.
 */
trait RouteFactory {
  this: Controller =>

  // Curry causes problem: ambiguous reference to overloaded definition
  // http://www.scala-lang.org/node/1360

  //----------------------------------------------------------------------------

  /**
   * Creates route which is not for direct routing for HTTP client,
   * like postback, 404 or 500 error handler.
   */
  def indirectRoute(body: => Unit) = Route(null, null, null, () => body, null, 0)

  //----------------------------------------------------------------------------

  // first and last should be lazy val or def so that they are run after
  // pathPrefix has been set

  def first = Route(null, RouteOrder.FIRST, PathPrefix.toCompiledPattern(pathPrefix), null, null, 0)
  def last  = Route(null, RouteOrder.LAST,  PathPrefix.toCompiledPattern(pathPrefix), null, null, 0)

  def cacheActionSecond(seconds: Int) = Route(null, RouteOrder.OTHER, PathPrefix.toCompiledPattern(pathPrefix), null, null, -seconds)
  def cacheActionMinute(minutes: Int) = Route(null, RouteOrder.OTHER, PathPrefix.toCompiledPattern(pathPrefix), null, null, -minutes * 60)
  def cacheActionHour  (hours:   Int) = Route(null, RouteOrder.OTHER, PathPrefix.toCompiledPattern(pathPrefix), null, null, -hours * 60 * 60)
  def cacheActionDay   (days:    Int) = Route(null, RouteOrder.OTHER, PathPrefix.toCompiledPattern(pathPrefix), null, null, -days * 60 * 60 * 24)

  def cachePageSecond(seconds: Int) = Route(null, RouteOrder.OTHER, PathPrefix.toCompiledPattern(pathPrefix), null, null, seconds)
  def cachePageMinute(minutes: Int) = Route(null, RouteOrder.OTHER, PathPrefix.toCompiledPattern(pathPrefix), null, null, minutes * 60)
  def cachePageHour  (hours:   Int) = Route(null, RouteOrder.OTHER, PathPrefix.toCompiledPattern(pathPrefix), null, null, hours * 60 * 60)
  def cachePageDay   (days:    Int) = Route(null, RouteOrder.OTHER, PathPrefix.toCompiledPattern(pathPrefix), null, null, days * 60 * 60 * 24)

  //----------------------------------------------------------------------------

  private def withPathPrefix(pattern: String) = {
    if (pathPrefix.isEmpty) pattern else pathPrefix + "/" + pattern
  }

  def GET(pattern: String = "")(body: => Unit) =
    Route(HttpMethod.GET, RouteOrder.OTHER, Routes.compilePattern(withPathPrefix(pattern)), () => body, null, 0)

  def POST(pattern: String = "")(body: => Unit) =
    Route(HttpMethod.POST, RouteOrder.OTHER, Routes.compilePattern(withPathPrefix(pattern)), () => body, null, 0)

  def PUT(pattern: String = "")(body: => Unit) =
    Route(HttpMethod.PUT, RouteOrder.OTHER, Routes.compilePattern(withPathPrefix(pattern)), () => body, null, 0)

  def DELETE(pattern: String = "")(body: => Unit) =
    Route(HttpMethod.DELETE, RouteOrder.OTHER, Routes.compilePattern(withPathPrefix(pattern)), () => body, null, 0)

  def WEBSOCKET(pattern: String = "")(body: => Unit) =
    Route(HttpMethodWebSocket, RouteOrder.OTHER, Routes.compilePattern(withPathPrefix(pattern)), () => body, null, 0)
}

object PathPrefix {
  // Used by RouteFactory
  def toCompiledPattern(pathPrefix: String) = Seq((pathPrefix, true))

  // Used by Route
  def fromCompiledPattern(compiledPattern: Seq[(String, Boolean)]) = compiledPattern.head._1
}
