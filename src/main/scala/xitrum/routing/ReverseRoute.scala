package xitrum.routing

import scala.annotation.tailrec

object ReverseRoute {
  def apply(routes: Seq[Route]): ReverseRoute = {
    val routesReverseSortedByNumPlaceholders = routes.sortBy(- _.numPlaceholders)
    new ReverseRoute(routesReverseSortedByNumPlaceholders)
  }

  @tailrec
  def collectReverseTokens(reverseTokens: Seq[Any], routeTokens: Seq[RouteToken], params: Map[String, Any]):
      Either[String, (Seq[Any], Map[String, Any])] =
  {
    if (routeTokens.isEmpty) {
      Right((reverseTokens, params))
    } else {
      routeTokens.head.url(params) match {
        case Left(e) => Left(e)

        case Right((reverseToken, remainingParams)) =>
          collectReverseTokens(reverseTokens :+ reverseToken, routeTokens.tail, remainingParams)
      }
    }
  }
}

/**
 * Routes are sorted reveresly by the number of placeholders because we want to
 * fill as many placeholders as possible.
 */
class ReverseRoute(routesReverseSortedByNumPlaceholders: Seq[Route]) {
  def url(params: Map[String, Any]): String = {
    var errorMsgs = Seq.empty[String]
    routesReverseSortedByNumPlaceholders.foreach { r =>
      r.url(params) match {
        case Left(errorMsg) =>
          errorMsgs = errorMsgs :+ errorMsg

        case Right(ret) =>
          return ret
      }
    }
    throw new Exception(errorMsgs.mkString(", "))
  }
}
