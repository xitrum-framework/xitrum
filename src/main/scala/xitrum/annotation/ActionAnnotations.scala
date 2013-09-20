package xitrum.annotation

import scala.reflect.runtime.universe

case class ActionAnnotations(
  route:      Option[universe.Annotation],
  routeOrder: Option[universe.Annotation],

  sockJsCookieNeeded: Option[universe.Annotation],
  sockJsNoWebSocket:  Option[universe.Annotation],

  error: Option[universe.Annotation],

  cache: Option[universe.Annotation],

  swagger: Option[universe.Annotation]
)
