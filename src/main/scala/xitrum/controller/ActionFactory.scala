package xitrum.controller

import java.lang.reflect.Method
import io.netty.handler.codec.http.HttpMethod

import xitrum.Controller
import xitrum.routing.{HttpMethodWebSocket, Route, RouteOrder, Routes}

/**
 * val action1 = GET("pattern") {
 * }
 *
 * val action2 = GET() {
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

  /**
   * Creates route which is not for direct routing for HTTP clients,
   * like 404 or 500 error handler.
   */
  def indirectAction(body: => Unit) = Action(null, () => body, null, 0)

  //----------------------------------------------------------------------------

  // first and last should be lazy val or def so that they are run after
  // pathPrefix has been set

  def first = Action(Route(null, RouteOrder.FIRST, PathPrefix.toCompiledPattern(pathPrefix)), null, null, 0)
  def last  = Action(Route(null, RouteOrder.LAST,  PathPrefix.toCompiledPattern(pathPrefix)), null, null, 0)

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
    Action(Route(HttpMethod.GET, RouteOrder.OTHER, Routes.compilePattern(withPathPrefix(pattern))), () => body, null, 0)

  def GET(body: => Any) =
    Action(Route(HttpMethod.GET, RouteOrder.OTHER, Routes.compilePattern(withPathPrefix(""))), () => body, null, 0)

  def POST(pattern: String)(body: => Any) =
    Action(Route(HttpMethod.POST, RouteOrder.OTHER, Routes.compilePattern(withPathPrefix(pattern))), () => body, null, 0)

  def POST(body: => Any) =
    Action(Route(HttpMethod.POST, RouteOrder.OTHER, Routes.compilePattern(withPathPrefix(""))), () => body, null, 0)

  def PUT(pattern: String)(body: => Any) =
    Action(Route(HttpMethod.PUT, RouteOrder.OTHER, Routes.compilePattern(withPathPrefix(pattern))), () => body, null, 0)

  def PUT(body: => Any) =
    Action(Route(HttpMethod.PUT, RouteOrder.OTHER, Routes.compilePattern(withPathPrefix(""))), () => body, null, 0)

  def DELETE(pattern: String)(body: => Any) =
    Action(Route(HttpMethod.DELETE, RouteOrder.OTHER, Routes.compilePattern(withPathPrefix(pattern))), () => body, null, 0)

  def DELETE(body: => Any) =
    Action(Route(HttpMethod.DELETE, RouteOrder.OTHER, Routes.compilePattern(withPathPrefix(""))), () => body, null, 0)

  def WEBSOCKET(pattern: String)(body: => Any) =
    Action(Route(HttpMethodWebSocket, RouteOrder.OTHER, Routes.compilePattern(withPathPrefix(pattern))), () => body, null, 0)

  def WEBSOCKET(body: => Any) =
    Action(Route(HttpMethodWebSocket, RouteOrder.OTHER, Routes.compilePattern(withPathPrefix(""))), () => body, null, 0)

  //----------------------------------------------------------------------------

  /**
   * Action in this same controller instance but not the currentAction will have
   * null method.
   *
   * In that case, to create new controller instance or get controller
   * class name & route name, call this method. It falls back to using
   * reflection to find inside this controller instance.
   */
  def nonNullMethodFromAction(action: Action): Method = {
    // Action in controller companion object is OK, see
    // ControllerReflection.cacheActionMethodToActionInCompanionControllerObject
    if (action.method != null)  // currentAction
      action.method
    else
      lookupMethodForActionWithNullMethod(action)
  }

  private def lookupMethodForActionWithNullMethod(action: Action): Method = synchronized {
    // Use reflection on this controller to find, and cache the result if any
    // Cannot use getFields because route fields are "val"s which are private in Java
    // Must use getDeclaredFields and set fields to public
    val controllerClass = getClass
    val fields          = controllerClass.getDeclaredFields
    fields.foreach { field =>
      if (field.getType == classOf[Action]) {
        field.setAccessible(true)
        val any = field.get(this)
        if (any == action) {
          val methodName = field.getName
          val method = controllerClass.getMethod(methodName)
          action.method = method  // Cache it
          return method
        }
      }
    }
    null
  }
}

object PathPrefix {
  // Used by ActionFactory
  def toCompiledPattern(pathPrefix: String) = Seq((pathPrefix, true))

  // Used by Action
  def fromCompiledPattern(compiledPattern: Seq[(String, Boolean)]) = compiledPattern.head._1
}
