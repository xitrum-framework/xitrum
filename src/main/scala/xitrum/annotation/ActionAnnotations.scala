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

  val TYPE_OF_GET      : universe.Type = universe.typeOf[GET]
  val TYPE_OF_POST     : universe.Type = universe.typeOf[POST]
  val TYPE_OF_PUT      : universe.Type = universe.typeOf[PUT]
  val TYPE_OF_PATCH    : universe.Type = universe.typeOf[PATCH]
  val TYPE_OF_DELETE   : universe.Type = universe.typeOf[DELETE]
  val TYPE_OF_WEBSOCKET: universe.Type = universe.typeOf[WEBSOCKET]
  val TYPE_OF_SOCKJS   : universe.Type = universe.typeOf[SOCKJS]

  val TYPE_OF_FIRST: universe.Type = universe.typeOf[First]
  val TYPE_OF_LAST : universe.Type = universe.typeOf[Last]

  val TYPE_OF_CACHE_ACTION_DAY   : universe.Type = universe.typeOf[CacheActionDay]
  val TYPE_OF_CACHE_ACTION_HOUR  : universe.Type = universe.typeOf[CacheActionHour]
  val TYPE_OF_CACHE_ACTION_MINUTE: universe.Type = universe.typeOf[CacheActionMinute]
  val TYPE_OF_CACHE_ACTION_SECOND: universe.Type = universe.typeOf[CacheActionSecond]

  val TYPE_OF_CACHE_PAGE_DAY   : universe.Type = universe.typeOf[CachePageDay]
  val TYPE_OF_CACHE_PAGE_HOUR  : universe.Type = universe.typeOf[CachePageHour]
  val TYPE_OF_CACHE_PAGE_MINUTE: universe.Type = universe.typeOf[CachePageMinute]
  val TYPE_OF_CACHE_PAGE_SECOND: universe.Type = universe.typeOf[CachePageSecond]

  val TYPE_OF_ROUTE      : universe.Type = universe.typeOf[Route]
  val TYPE_OF_ROUTE_ORDER: universe.Type = universe.typeOf[RouteOrder]
  val TYPE_OF_ERROR      : universe.Type = universe.typeOf[Error]
  val TYPE_OF_CACHE      : universe.Type = universe.typeOf[Cache]

  val TYPE_OF_SWAGGER: universe.Type = universe.typeOf[Swagger]

  val TYPE_OF_SOCKJS_COOKIE_NEEDED   : universe.Type = universe.typeOf[SockJsCookieNeeded]
  val TYPE_OF_SOCKJS_NO_COOKIE_NEEDED: universe.Type = universe.typeOf[SockJsNoCookieNeeded]
  val TYPE_OF_SOCKJS_NO_WEBSOCKET    : universe.Type = universe.typeOf[SockJsNoWebSocket]

  val TYPE_OF_ERROR_404: universe.Type = universe.typeOf[Error404]
  val TYPE_OF_ERROR_500: universe.Type = universe.typeOf[Error500]

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
  def inherit(ancestor: ActionAnnotations): ActionAnnotations = ActionAnnotations(
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
