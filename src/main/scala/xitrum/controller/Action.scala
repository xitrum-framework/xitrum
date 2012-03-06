package xitrum.controller

import java.lang.reflect.Method
import io.netty.handler.codec.http.HttpMethod

import xitrum.Controller
import xitrum.routing.{Route, RouteCompiler, RouteOrder, Routes, HttpMethodWebSocket}

/**
 * @param method    for creating new controller instance,
 *                  or getting controller class name and action method name,
 *                  is a var because it will be updated for caching
 * @param cacheSecs 0 = no cache, < 0 = cache action, > 0 = cache page
 *
 * See ActionFactory, methods here relates to those there.
 */
case class Action(route: Route, var method: Method, body: () => Unit, cacheSeconds: Int) {
  def first = Action(Route(route.httpMethod, RouteOrder.FIRST, route.compiledPattern), method, body, cacheSeconds)
  def last  = Action(Route(route.httpMethod, RouteOrder.LAST,  route.compiledPattern), method, body, cacheSeconds)

  def cacheActionSecond(seconds: Int) = Action(route, method, body, -seconds)
  def cacheActionMinute(minutes: Int) = Action(route, method, body, -minutes * 60)
  def cacheActionHour  (hours:   Int) = Action(route, method, body, -hours * 60 * 60)
  def cacheActionDay   (days:    Int) = Action(route, method, body, -days * 60 * 60 * 24)

  def cachePageSecond(seconds: Int) = Action(route, method, body, seconds)
  def cachePageMinute(minutes: Int) = Action(route, method, body, minutes * 60)
  def cachePageHour  (hours:   Int) = Action(route, method, body, hours * 60 * 60)
  def cachePageDay   (days:    Int) = Action(route, method, body, days * 60 * 60 * 24)

  //----------------------------------------------------------------------------

  private def withPathPrefix(pattern: String) = {
    val pathPrefix = PathPrefix.fromCompiledPattern(route.compiledPattern)
    if (pathPrefix.isEmpty) pattern else pathPrefix + "/" + pattern
  }

  def GET(pattern: String)(body: => Any) =
    Action(Route(HttpMethod.GET, route.order, RouteCompiler.compile(withPathPrefix(pattern))), method, () => body, cacheSeconds)

  def GET(body: => Any) =
    Action(Route(HttpMethod.GET, route.order, RouteCompiler.compile(withPathPrefix(""))), method, () => body, cacheSeconds)

  def POST(pattern: String)(body: => Any) =
    Action(Route(HttpMethod.POST, route.order, RouteCompiler.compile(withPathPrefix(pattern))), method, () => body, cacheSeconds)

  def POST(body: => Any) =
    Action(Route(HttpMethod.POST, route.order, RouteCompiler.compile(withPathPrefix(""))), method, () => body, cacheSeconds)

  def PUT(pattern: String)(body: => Any) =
    Action(Route(HttpMethod.PUT, route.order, RouteCompiler.compile(withPathPrefix(pattern))), method, () => body, cacheSeconds)

  def PUT(body: => Any) =
    Action(Route(HttpMethod.PUT, route.order, RouteCompiler.compile(withPathPrefix(""))), method, () => body, cacheSeconds)

  def DELETE(pattern: String)(body: => Any) =
    Action(Route(HttpMethod.DELETE, route.order, RouteCompiler.compile(withPathPrefix(pattern))), method, () => body, cacheSeconds)

  def DELETE(body: => Any) =
    Action(Route(HttpMethod.DELETE, route.order, RouteCompiler.compile(withPathPrefix(""))), method, () => body, cacheSeconds)

  def WEBSOCKET(pattern: String)(body: => Any) =
    Action(Route(HttpMethodWebSocket, route.order, RouteCompiler.compile(withPathPrefix(pattern))), method, () => body, cacheSeconds)

  def WEBSOCKET(body: => Any) =
    Action(Route(HttpMethodWebSocket, route.order, RouteCompiler.compile(withPathPrefix(""))), method, () => body, cacheSeconds)

  //----------------------------------------------------------------------------

  def url(params: (String, Any)*) = route.url(params:_*)
  lazy val url: String = url()

  def absoluteUrl(params: (String, Any)*)(implicit controller: Controller) = controller.absoluteUrlPrefix + url(params:_*)
  def absoluteUrl(implicit controller: Controller): String = absoluteUrl()(controller)
}
