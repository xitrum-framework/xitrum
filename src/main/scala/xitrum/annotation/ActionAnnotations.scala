package xitrum.annotation

import scala.reflect.runtime.universe

// http://docs.scala-lang.org/overviews/reflection/annotations-names-scopes.html
// http://www.veebsbraindump.com/2013/01/reflecting-annotations-in-scala-2-10/

object ActionAnnotations {
  // ==
  //
  // Can't use, for example, tpe == TYPE_OF_GET in development mode because
  // new class loader may be created in development mode, in that case we
  // can't compare things in different universes.
  // => Must use .toString

  // <:<
  //
  // <:< still works for different universes!

  val TYPE_OF_GET       = universe.typeOf[GET]
  val TYPE_OF_POST      = universe.typeOf[POST]
  val TYPE_OF_PUT       = universe.typeOf[PUT]
  val TYPE_OF_PATCH     = universe.typeOf[PATCH]
  val TYPE_OF_DELETE    = universe.typeOf[DELETE]
  val TYPE_OF_WEBSOCKET = universe.typeOf[WEBSOCKET]
  val TYPE_OF_SOCKJS    = universe.typeOf[SOCKJS]

  val TYPE_OF_FIRST = universe.typeOf[First]
  val TYPE_OF_LAST  = universe.typeOf[Last]

  val TYPE_OF_CACHE_ACTION_DAY    = universe.typeOf[CacheActionDay]
  val TYPE_OF_CACHE_ACTION_HOUR   = universe.typeOf[CacheActionHour]
  val TYPE_OF_CACHE_ACTION_MINUTE = universe.typeOf[CacheActionMinute]
  val TYPE_OF_CACHE_ACTION_SECOND = universe.typeOf[CacheActionSecond]

  val TYPE_OF_CACHE_PAGE_DAY    = universe.typeOf[CachePageDay]
  val TYPE_OF_CACHE_PAGE_HOUR   = universe.typeOf[CachePageHour]
  val TYPE_OF_CACHE_PAGE_MINUTE = universe.typeOf[CachePageMinute]
  val TYPE_OF_CACHE_PAGE_SECOND = universe.typeOf[CachePageSecond]

  val TYPE_OF_ROUTE       = universe.typeOf[Route]
  val TYPE_OF_ROUTE_ORDER = universe.typeOf[RouteOrder]
  val TYPE_OF_ERROR       = universe.typeOf[Error]
  val TYPE_OF_CACHE       = universe.typeOf[Cache]

  val TYPE_OF_SWAGGER = universe.typeOf[Swagger]

  val TYPE_OF_SOCKJS_COOKIE_NEEDED    = universe.typeOf[SockJsCookieNeeded]
  val TYPE_OF_SOCKJS_NO_COOKIE_NEEDED = universe.typeOf[SockJsNoCookieNeeded]
  val TYPE_OF_SOCKJS_NO_WEBSOCKET     = universe.typeOf[SockJsNoWebSocket]

  val TYPE_OF_ERROR_404 = universe.typeOf[Error404]
  val TYPE_OF_ERROR_500 = universe.typeOf[Error500]

  def fromUniverse(annotations: Seq[universe.Annotation]): ActionAnnotations = {
    var ret = ActionAnnotations()

    annotations.foreach { a =>
      val tpe = a.tree.tpe

      if (tpe <:< TYPE_OF_ROUTE)
        ret = ret.copy(routes = ret.routes :+ a)

      else if (tpe <:< TYPE_OF_ROUTE_ORDER)
        ret = ret.copy(routeOrder = Some(a))

      else if (tpe <:< TYPE_OF_SOCKJS_COOKIE_NEEDED)
        ret = ret.copy(sockJsCookieNeeded = Some(a))

      else if (tpe <:< TYPE_OF_SOCKJS_NO_COOKIE_NEEDED)
        ret = ret.copy(sockJsNoCookieNeeded = Some(a))

      else if (tpe <:< TYPE_OF_SOCKJS_NO_WEBSOCKET)
        ret = ret.copy(sockJsNoWebSocket = Some(a))

      else if (tpe <:< TYPE_OF_ERROR)
        ret = ret.copy(error = Some(a))

      else if (tpe <:< TYPE_OF_CACHE)
        ret = ret.copy(cache = Some(a))

      else if (tpe <:< TYPE_OF_SWAGGER)
        ret = ret.copy(swaggers = ret.swaggers :+ a)
    }

    ret
  }
}

case class ActionAnnotations(
  routes:               Seq[universe.Annotation]    = Seq.empty,
  routeOrder:           Option[universe.Annotation] = None,
  sockJsCookieNeeded:   Option[universe.Annotation] = None,
  sockJsNoCookieNeeded: Option[universe.Annotation] = None,
  sockJsNoWebSocket:    Option[universe.Annotation] = None,
  error:                Option[universe.Annotation] = None,
  cache:                Option[universe.Annotation] = None,
  swaggers:             Seq[universe.Annotation]    = Seq.empty[universe.Annotation]
) {
  import ActionAnnotations._

  /**
   * inherit sockJsCookie, sockJsNoCookie, sockJsNoWebSocket, cache, and swaggers.
   * Do not inherit routes, routeOrder, and error.
   * Current values if exist will override those in ancestor.
   */
  def inherit(ancestor: ActionAnnotations) = ActionAnnotations(
    routes,
    routeOrder,
    sockJsCookieNeeded   orElse ancestor.sockJsCookieNeeded,
    sockJsNoCookieNeeded orElse ancestor.sockJsNoCookieNeeded,
    sockJsNoWebSocket    orElse ancestor.sockJsNoWebSocket,
    error,
    cache                orElse ancestor.cache,
    ancestor.swaggers ++ swaggers
  )

  /**
   * Only inherit sockJsCookie, sockJsNoCookie, sockJsNoWebSocket, cache, and swaggers.
   * Do not inherit routes, routeOrder, and error.
   * Current values if exist will override those in ancestor.
   */
  def inherit(annotations: Seq[universe.Annotation]): ActionAnnotations = {
    var ret = this
    annotations.foreach { a =>
      val tpe = a.tree.tpe

      if (sockJsCookieNeeded.isEmpty && tpe <:< TYPE_OF_SOCKJS_COOKIE_NEEDED)
        ret = ret.copy(sockJsCookieNeeded = Some(a))

      if (sockJsNoCookieNeeded.isEmpty && tpe <:< TYPE_OF_SOCKJS_NO_COOKIE_NEEDED)
        ret = ret.copy(sockJsNoCookieNeeded = Some(a))

      else if (sockJsNoWebSocket.isEmpty && tpe <:< TYPE_OF_SOCKJS_NO_WEBSOCKET)
        ret = ret.copy(sockJsNoWebSocket = Some(a))

      else if (cache.isEmpty && tpe <:< TYPE_OF_CACHE)
        ret = ret.copy(cache = Some(a))

      else if (tpe <:< TYPE_OF_SWAGGER)
        ret = ret.copy(swaggers = a +: ret.swaggers)
    }
    ret
  }
}
