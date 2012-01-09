package xitrum.controller

import java.lang.reflect.Method
import io.netty.handler.codec.http.HttpMethod

import xitrum.Controller
import xitrum.routing.{Route, RouteOrder, Routes, HttpMethodWebSocket}

/**
 * @param method    for creating new controller instance,
 *                  or getting controller class name and route method name,
 *                  is a var because it will be updated for caching
 * @param cacheSecs 0 = no cache, < 0 = cache action, > 0 = cache page
 *
 * See ActionFactory, methods here relates to those there.
 */
case class Action(route: Route, body: () => Unit, var method: Method, cacheSeconds: Int) {
  def first = Action(Route(route.httpMethod, RouteOrder.FIRST, route.compiledPattern), body, method, cacheSeconds)
  def last  = Action(Route(route.httpMethod, RouteOrder.LAST,  route.compiledPattern), body, method, cacheSeconds)

  def cacheActionSecond(seconds: Int) = Action(route, body, method, -seconds)
  def cacheActionMinute(minutes: Int) = Action(route, body, method, -minutes * 60)
  def cacheActionHour  (hours:   Int) = Action(route, body, method, -hours * 60 * 60)
  def cacheActionDay   (days:    Int) = Action(route, body, method, -days * 60 * 60 * 24)

  def cachePageSecond(seconds: Int) = Action(route, body, method, seconds)
  def cachePageMinute(minutes: Int) = Action(route, body, method, minutes * 60)
  def cachePageHour  (hours:   Int) = Action(route, body, method, hours * 60 * 60)
  def cachePageDay   (days:    Int) = Action(route, body, method, days * 60 * 60 * 24)

  //----------------------------------------------------------------------------

  private def withPathPrefix(pattern: String) = {
    val pathPrefix = PathPrefix.fromCompiledPattern(route.compiledPattern)
    if (pathPrefix.isEmpty) pattern else pathPrefix + "/" + pattern
  }

  def GET(pattern: String)(body: => Any) =
    Action(Route(HttpMethod.GET, route.order, Routes.compilePattern(withPathPrefix(pattern))), () => body, method, cacheSeconds)

  def GET(body: => Any) =
    Action(Route(HttpMethod.GET, route.order, Routes.compilePattern(withPathPrefix(""))), () => body, method, cacheSeconds)

  def POST(pattern: String)(body: => Any) =
    Action(Route(HttpMethod.POST, route.order, Routes.compilePattern(withPathPrefix(pattern))), () => body, method, cacheSeconds)

  def POST(body: => Any) =
    Action(Route(HttpMethod.POST, route.order, Routes.compilePattern(withPathPrefix(""))), () => body, method, cacheSeconds)

  def PUT(pattern: String)(body: => Any) =
    Action(Route(HttpMethod.PUT, route.order, Routes.compilePattern(withPathPrefix(pattern))), () => body, method, cacheSeconds)

  def PUT(body: => Any) =
    Action(Route(HttpMethod.PUT, route.order, Routes.compilePattern(withPathPrefix(""))), () => body, method, cacheSeconds)

  def DELETE(pattern: String)(body: => Any) =
    Action(Route(HttpMethod.DELETE, route.order, Routes.compilePattern(withPathPrefix(pattern))), () => body, method, cacheSeconds)

  def DELETE(body: => Any) =
    Action(Route(HttpMethod.DELETE, route.order, Routes.compilePattern(withPathPrefix(""))), () => body, method, cacheSeconds)

  def WEBSOCKET(pattern: String)(body: => Any) =
    Action(Route(HttpMethodWebSocket, route.order, Routes.compilePattern(withPathPrefix(pattern))), () => body, method, cacheSeconds)

  def WEBSOCKET(body: => Any) =
    Action(Route(HttpMethodWebSocket, route.order, Routes.compilePattern(withPathPrefix(""))), () => body, method, cacheSeconds)

  //----------------------------------------------------------------------------

  def url(params: (String, Any)*) = route.url(params:_*)
  lazy val url: String = url()
}
