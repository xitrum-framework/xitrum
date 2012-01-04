package xitrum.controller

import java.lang.reflect.Method
import scala.collection.mutable.{Map => MMap}
import io.netty.handler.codec.http.HttpMethod

import xitrum.Controller
import xitrum.routing.{HttpMethodWebSocket, Route, RouteOrder, Routes, ControllerReflection}

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

  //----------------------------------------------------------------------------

  /**
   * Route in controller companion object or in this same controller but
   * not the currentRoute will have null routeMethod.
   *
   * In that case, to create new controller instance or get controller
   * class name & route name, call this method. It calls
   * ControllerReflection.lookupRouteMethodForRouteWithNullRouteMethod
   * (for route in controller companion object) then falls back to using
   * reflection to find inside this controller.
   */
  def nonNullRouteMethodFromRoute(route: Route): Method = {
    if (route.routeMethod != null) {  // currentRoute
      route.routeMethod
    } else {
      val fromControllerReflection = ControllerReflection.lookupRouteMethodForRouteWithNullRouteMethod(route)
      if (fromControllerReflection != null)
        fromControllerReflection
      else
        lookupRouteMethodForRouteWithNullRouteMethod(route)
    }
  }

  private val routeWithNullRouteMethod_to_RouteMethod_cache = MMap[Route, Method]()

  private def lookupRouteMethodForRouteWithNullRouteMethod(route: Route): Method = synchronized {
    if (routeWithNullRouteMethod_to_RouteMethod_cache.isDefinedAt(route))
      return routeWithNullRouteMethod_to_RouteMethod_cache(route)

    // Use reflection on this controller to find, and cache the result if any
    // Cannot use getFields because route fields are "val"s which are private in Java
    // Must use getDeclaredFields and set fields to public
    val controllerClass = getClass
    val fields          = controllerClass.getDeclaredFields
    fields.foreach { field =>
      if (field.getType == classOf[Route]) {
        field.setAccessible(true)
        val any = field.get(this)
        if (any == route) {
          val methodName = field.getName
          val routeMethod = controllerClass.getMethod(methodName)
          routeWithNullRouteMethod_to_RouteMethod_cache(route) = routeMethod  // Cache it
          return routeMethod
        }
      }
    }
    null
  }
}

object PathPrefix {
  // Used by RouteFactory
  def toCompiledPattern(pathPrefix: String) = Seq((pathPrefix, true))

  // Used by Route
  def fromCompiledPattern(compiledPattern: Seq[(String, Boolean)]) = compiledPattern.head._1
}
