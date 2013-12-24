package xitrum.annotation

import scala.reflect.runtime.universe

// http://docs.scala-lang.org/overviews/reflection/annotations-names-scopes.html
// http://www.veebsbraindump.com/2013/01/reflecting-annotations-in-scala-2-10/

object ActionAnnotations {
  val typeOfRoute              = universe.typeOf[Route]
  val typeOfRouteOrder         = universe.typeOf[RouteOrder]
  val typeOfSockJsCookieNeeded = universe.typeOf[SockJsCookieNeeded]
  val typeOfSockJsNoWebSocket  = universe.typeOf[SockJsNoWebSocket]
  val typeOfError              = universe.typeOf[Error]
  val typeOfCache              = universe.typeOf[Cache]

  val typeOfSwagger = universe.typeOf[Swagger]

  val typeOfError404 = universe.typeOf[Error404]
  val typeOfError500 = universe.typeOf[Error500]

  val typeOfGET       = universe.typeOf[GET]
  val typeOfPOST      = universe.typeOf[POST]
  val typeOfPUT       = universe.typeOf[PUT]
  val typeOfPATCH     = universe.typeOf[PATCH]
  val typeOfDELETE    = universe.typeOf[DELETE]
  val typeOfWEBSOCKET = universe.typeOf[WEBSOCKET]
  val typeOfSOCKJS    = universe.typeOf[SOCKJS]

  val typeOfFirst = universe.typeOf[First]
  val typeOfLast  = universe.typeOf[Last]

  val typeOfCacheActionDay    = universe.typeOf[CacheActionDay]
  val typeOfCacheActionHour   = universe.typeOf[CacheActionHour]
  val typeOfCacheActionMinute = universe.typeOf[CacheActionMinute]
  val typeOfCacheActionSecond = universe.typeOf[CacheActionSecond]

  val typeOfCachePageDay    = universe.typeOf[CachePageDay]
  val typeOfCachePageHour   = universe.typeOf[CachePageHour]
  val typeOfCachePageMinute = universe.typeOf[CachePageMinute]
  val typeOfCachePageSecond = universe.typeOf[CachePageSecond]

  def fromUniverse(annotations: Seq[universe.Annotation]): ActionAnnotations = {
    var ret = ActionAnnotations()

    annotations.foreach { a =>
      val tpe = a.tpe

      if (tpe <:< typeOfRoute)
        ret = ret.copy(routes = ret.routes :+ a)

      else if (tpe <:< typeOfRouteOrder)
        ret = ret.copy(routeOrder = Some(a))

      else if (tpe <:< typeOfSockJsCookieNeeded)
        ret = ret.copy(sockJsCookieNeeded = Some(a))

      else if (tpe <:< typeOfSockJsNoWebSocket)
        ret = ret.copy(sockJsNoWebSocket = Some(a))

      else if (tpe <:< typeOfError)
        ret = ret.copy(error = Some(a))

      else if (tpe <:< typeOfCache)
        ret = ret.copy(cache = Some(a))

      else if (tpe <:< typeOfSwagger)
        ret = ret.copy(swaggers = ret.swaggers :+ a)
    }

    ret
  }
}

case class ActionAnnotations(
  routes:     Seq[universe.Annotation] = Seq.empty,
  routeOrder: Option[universe.Annotation] = None,

  sockJsCookieNeeded: Option[universe.Annotation] = None,
  sockJsNoWebSocket:  Option[universe.Annotation] = None,

  error: Option[universe.Annotation] = None,

  cache: Option[universe.Annotation] = None,

  swaggers: Seq[universe.Annotation] = Seq.empty[universe.Annotation]
) {
  import ActionAnnotations._

  /**
   * Only inherit sockJsCookieNeeded, sockJsNoWebSocket, cache, and swaggers.
   * Do not inherit routes, routeOrder, and error.
   * Current values if exist will override those in ancestor.
   */
  def inherit(ancestor: ActionAnnotations) = ActionAnnotations(
    routes,
    routeOrder,
    sockJsCookieNeeded orElse ancestor.sockJsCookieNeeded,
    sockJsNoWebSocket  orElse ancestor.sockJsNoWebSocket,
    error,
    cache              orElse ancestor.cache,
    ancestor.swaggers ++ swaggers
  )

  /**
   * Only inherit sockJsCookieNeeded, sockJsNoWebSocket, cache, and swaggers.
   * Do not inherit routes, routeOrder, and error.
   * Current values if exist will override those in ancestor.
   */
  def inherit(annotations: Seq[universe.Annotation]): ActionAnnotations = {
    var ret = this
    annotations.foreach { a =>
      val tpe = a.tpe

      if (sockJsCookieNeeded.isEmpty && tpe <:< typeOfSockJsCookieNeeded)
        ret = ret.copy(sockJsCookieNeeded = Some(a))

      else if (sockJsNoWebSocket.isEmpty && tpe <:< typeOfSockJsNoWebSocket)
        ret = ret.copy(sockJsNoWebSocket = Some(a))

      else if (cache.isEmpty && tpe <:< typeOfCache)
        ret = ret.copy(cache = Some(a))

      else if (tpe <:< typeOfSwagger)
        ret = ret.copy(swaggers = a +: ret.swaggers)
    }
    ret
  }
}
