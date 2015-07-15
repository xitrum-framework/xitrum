package xitrum.routing

import scala.annotation.tailrec
import scala.util.matching.Regex

import xitrum.scope.request.Params

sealed trait RouteToken {
  /**
   * Reconstruct NonDotRouteToken("id", true, Some("[0-9]+".r)):
   * - Not for Swagger => ":id<[0-9]+>"
   * - For Swagger     => {id}
   */
  def decompile(forSwagger: Boolean): String

  /**
   * Used by ReverseRoute
   * (reverse routes are cached, thus no need to cache numPlaceholders as "val")
   */
  def numPlaceholders: Int

  /**
   * @param params Used when isPlaceholder is true
   *
   * @return Left(error) or Right((token value, remaining params))
   */
  def url(params: Map[String, Any]): Either[String, (Any, Map[String, Any])]

  /**
   * @param pathParams May be updated if matched
   *
   * @param last Used when matching "*"
   */
  def matchToken(pathParams: Params, pathTokens: Seq[String], last: Boolean): Boolean
}

/**
 * "articles/:id<[0-9]+>" gives 2 tokens:
 * - NonDotRouteToken("articles", false, None)
 * - NonDotRouteToken("id",       true,  Some("[0-9]+".r))
 */
case class NonDotRouteToken(value: String, isPlaceholder: Boolean, regex: Option[Regex]) extends RouteToken {
  def decompile(forSwagger: Boolean): String = {
    if (forSwagger) {
      if (isPlaceholder) "{" + value + "}" else value
    } else {
      val rawValue = if (isPlaceholder) ":" + value else value
      val rawRegex = regex match {
        case None => ""
        case Some(r) =>
          val string                = r.toString
          val withoutGraveAndDollar = string.substring(1, string.length - 1)
          "<" + withoutGraveAndDollar + ">"
      }
      rawValue + rawRegex
    }
  }

  def numPlaceholders = if (isPlaceholder) 1 else 0

  def url(params: Map[String, Any]) = {
    if (isPlaceholder) {
      if (params.isDefinedAt(value))
        Right((params(value), params - value))
      else
        Left("Cannot create reverse URL because there's no required key: \"" + value + "\"")
    } else {
      Right((value, params))
    }
  }

  def matchToken(pathParams: Params, pathTokens: Seq[String], last: Boolean): Boolean = {
    if (isPlaceholder) {
      if (last) {
        if (value == "*") {
          val value = pathTokens.mkString("/")
          matchRegex(pathParams, value)
        } else {
          if (pathTokens.length > 1) {
            false
          } else {
            // Placeholder in URL can't be empty
            val value = pathTokens.head
            if (value.length == 0) false else matchRegex(pathParams, value)
          }
        }
      } else {
        if (value == "*") {
          false
        } else {
          // Placeholder in URL can't be empty
          val value = pathTokens.head
          if (value.length == 0) false else matchRegex(pathParams, value)
        }
      }
    } else {
      pathTokens.head == value
    }
  }

  /** pathParams is updated */
  private def matchRegex(pathParams: Params, value: String): Boolean = {
    regex match {
      case None =>
        pathParams(this.value) = Seq(value)
        true

      case Some(r) =>
        r.findFirstIn(value) match {
          case None =>
            false

          case _ =>
            pathParams(this.value) = Seq(value)
            true
        }
    }
  }
}

/**
 * "articles/:id<[0-9]+>.:format" gives 2 tokens:
 * - NonDotRouteToken("articles", false, None)
 * - DotRouteToken(Seq(NonDotRouteToken("id", true, Some("[0-9]+".r)), NonDotRouteToken("format", true, None)))
 */
case class DotRouteToken(nonDotRouteTokens: Seq[NonDotRouteToken]) extends RouteToken {
  def decompile(forSwagger: Boolean): String = nonDotRouteTokens.map(_.decompile(forSwagger)).mkString(".")

  def numPlaceholders = nonDotRouteTokens.foldLeft(0) { (sum, rt) => sum + rt.numPlaceholders }

  def url(params: Map[String, Any]) = {
    ReverseRoute.collectReverseTokens(Seq.empty[String], nonDotRouteTokens, params) match {
      case Left(e) => Left(e)

      case Right((dots, remainingParams)) =>
        Right((dots.mkString("."), remainingParams))
    }
  }

  def matchToken(pathParams: Params, pathTokens: Seq[String], last: Boolean): Boolean = {
    val ts = pathTokens.head.split('.')  // TODO: How to reuse this?
    if (ts.length == nonDotRouteTokens.length)
      matchNonDotRouteTokens(pathParams, ts, nonDotRouteTokens)
    else
      false
  }

  /** pathTokens and nonDotRouteTokens are of the same length. */
  @tailrec
  private def matchNonDotRouteTokens(pathParams: Params, pathTokens: Seq[String], nonDotRouteTokens: Seq[NonDotRouteToken]): Boolean = {
    if (pathTokens.isEmpty) {
      true
    } else {
      val matched = nonDotRouteTokens.head.matchToken(pathParams, Seq(pathTokens.head), false)
      if (matched)
        matchNonDotRouteTokens(pathParams, pathTokens.tail, nonDotRouteTokens.tail)
      else
        false
    }
  }
}
