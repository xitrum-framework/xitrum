package xitrum.annotation

import scala.reflect.runtime.universe

case class ActionAnnotations(
  route:      Option[universe.Annotation] = None,
  routeOrder: Option[universe.Annotation] = None,

  sockJsCookieNeeded: Option[universe.Annotation] = None,
  sockJsNoWebSocket:  Option[universe.Annotation] = None,

  error: Option[universe.Annotation] = None,

  cache: Option[universe.Annotation] = None,

  swagger: Option[universe.Annotation] = None
) {
  def overrideMe(other: ActionAnnotations) = ActionAnnotations(
    other.route              orElse route,
    other.routeOrder         orElse routeOrder,
    other.sockJsCookieNeeded orElse sockJsCookieNeeded,
    other.sockJsNoWebSocket  orElse sockJsNoWebSocket,
    other.error              orElse error,
    other.cache              orElse cache,
    other.swagger            orElse swagger
  )

  def overrideMe(annotations: Seq[universe.Annotation]): ActionAnnotations = {
    var ret = this
    annotations.foreach { a =>
      if (a.tpe <:< universe.typeOf[Route])
        ret = ret.copy(route = Some(a))

      else if (a.tpe <:< universe.typeOf[RouteOrder])
        ret = ret.copy(routeOrder = Some(a))

      else if (a.tpe <:< universe.typeOf[SockJsCookieNeeded])
        ret = ret.copy(sockJsCookieNeeded = Some(a))

      else if (a.tpe <:< universe.typeOf[SockJsNoWebSocket])
        ret = ret.copy(sockJsNoWebSocket = Some(a))

      else if (a.tpe <:< universe.typeOf[Error])
        ret = ret.copy(error = Some(a))

      else if (a.tpe <:< universe.typeOf[Cache])
        ret = ret.copy(cache = Some(a))

      else if (a.tpe <:< universe.typeOf[Swagger])
        ret = ret.copy(swagger = Some(a))
    }
    ret
  }
}
