package xitrum.routing

import java.lang.reflect.Method
import io.netty.handler.codec.http.{HttpMethod, QueryStringEncoder}

import xitrum.Config
import xitrum.controller.PathPrefix
import xitrum.util.SecureBase64

/**
 * @param routeMethod for creating new controller instance, see xitrum.routing.Routes
 * @param cacheSecs    0 = no cache, < 0 = cache action, > 0 = cache page
 *
 * See RouteFactory, methods here relates to those there.
 */
case class Route(httpMethod: HttpMethod, order: RouteOrder.RouteOrder, compiledPattern: CompiledPattern, body: () => Unit, routeMethod: Method, cacheSeconds: Int) {
  def first = Route(httpMethod, RouteOrder.FIRST, compiledPattern, body, routeMethod, cacheSeconds)
  def last  = Route(httpMethod, RouteOrder.LAST,  compiledPattern, body, routeMethod, cacheSeconds)

  def cacheActionSecond(seconds: Int) = Route(httpMethod, order, compiledPattern, body, routeMethod, -seconds)
  def cacheActionMinute(minutes: Int) = Route(httpMethod, order, compiledPattern, body, routeMethod, -minutes * 60)
  def cacheActionHour  (hours:   Int) = Route(httpMethod, order, compiledPattern, body, routeMethod, -hours * 60 * 60)
  def cacheActionDay   (days:    Int) = Route(httpMethod, order, compiledPattern, body, routeMethod, -days * 60 * 60 * 24)

  def cachePageSecond(seconds: Int) = Route(httpMethod, order, compiledPattern, body, routeMethod, seconds)
  def cachePageMinute(minutes: Int) = Route(httpMethod, order, compiledPattern, body, routeMethod, minutes * 60)
  def cachePageHour  (hours:   Int) = Route(httpMethod, order, compiledPattern, body, routeMethod, hours * 60 * 60)
  def cachePageDay   (days:    Int) = Route(httpMethod, order, compiledPattern, body, routeMethod, days * 60 * 60 * 24)

  //----------------------------------------------------------------------------

  private def withPathPrefix(pattern: String) = {
    val pathPrefix = PathPrefix.fromCompiledPattern(compiledPattern)
    if (pathPrefix.isEmpty) pattern else pathPrefix + "/" + pattern
  }

  def GET(pattern: String = "")(body: => Unit) =
    Route(HttpMethod.GET, order, Routes.compilePattern(withPathPrefix(pattern)), () => body, routeMethod, cacheSeconds)

  def POST(pattern: String = "")(body: => Unit) =
    Route(HttpMethod.POST, order, Routes.compilePattern(withPathPrefix(pattern)), () => body, routeMethod, cacheSeconds)

  def PUT(pattern: String = "")(body: => Unit) =
    Route(HttpMethod.PUT, order, Routes.compilePattern(withPathPrefix(pattern)), () => body, routeMethod, cacheSeconds)

  def DELETE(pattern: String = "")(body: => Unit) =
    Route(HttpMethod.DELETE, order, Routes.compilePattern(withPathPrefix(pattern)), () => body, routeMethod, cacheSeconds)

  def WEBSOCKET(pattern: String = "")(body: => Unit) =
    Route(HttpMethodWebSocket, order, Routes.compilePattern(withPathPrefix(pattern)), () => body, routeMethod, cacheSeconds)

  //----------------------------------------------------------------------------

  def url(params: (String, Any)*) = {
    var map = params.toMap
    val tokens = compiledPattern.map { case (token, constant) =>
      if (constant) {
        token
      } else {
        val ret = map(token)
        map = map - token
        ret
      }
    }
    val url = Config.withBaseUri("/" + tokens.mkString("/"))

    val qse = new QueryStringEncoder(url, Config.requestCharset)
    for ((k, v) <- map) qse.addParam(k, v.toString)
    qse.toString
  }

  lazy val url: String = url()

  lazy val postbackUrl: String = {
    val nonNullRouteMethod =
      if (routeMethod != null)  // Current route
        routeMethod
      else                      // Route from controller companion object has null routeMethod
        ControllerReflection.lookupRouteMethodForRouteWithNullRouteMethod(this)

    // routeMethod (thus Route) is not serializable
    // Use controllerRouteName instead
    val encryptedControllerRouteName = SecureBase64.encrypt(ControllerReflection.controllerRouteName(nonNullRouteMethod))
    PostbackController.postback.url("ecr" -> encryptedControllerRouteName)
  }
}
