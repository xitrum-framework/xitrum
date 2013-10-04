package xitrum.routing

import scala.util.matching.Regex

/**
 * "/articles/:id<[0-9]+>" gives 2 tokens:
 * RouteToken("articles", true, None) and
 * RouteToken("id", false, Some("[0-9]+".r))
 */
case class RouteToken(value: String, isPlaceholder: Boolean, regex: Option[Regex])
