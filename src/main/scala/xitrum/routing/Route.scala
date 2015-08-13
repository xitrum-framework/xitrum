package xitrum.routing

import scala.annotation.tailrec
import scala.collection.mutable.{Map => MMap}
import io.netty.handler.codec.http.{HttpMethod, QueryStringEncoder}

import xitrum.{Config, Action}
import xitrum.scope.request.Params

/** @param cacheSecs < 0: cache action, > 0: cache page, 0: no cache */
class Route(
  // In
  val httpMethod: HttpMethod, val compiledPattern: Seq[RouteToken],

  // Out
  val klass: Class[_ <: Action], val cacheSecs: Int
)
{
  def numPlaceholders = compiledPattern.foldLeft(0) { (sum, rt) => sum + rt.numPlaceholders }

  def url(params: Map[String, Any]): Either[String, String] = {
    ReverseRoute.collectReverseTokens(Seq.empty[String], compiledPattern, params) match {
      case Left(e) => Left(e)

      case Right((tokens, remainingParams)) =>
        val url = Config.withBaseUrl("/" + tokens.mkString("/"))

        // The remaining are put to query part on the URL
        val qse = new QueryStringEncoder(url, Config.xitrum.request.charset)
        for ((k, v) <- remainingParams) qse.addParam(k, v.toString)
        Right(qse.toString)
    }
  }

  /** @return None if not matched */
  def matchRoute(pathTokens: Seq[String]): Option[Params] = {
    val max1 = pathTokens.length
    val max2 = compiledPattern.length

    // Check the number of tokens
    // max2 must be <= max1
    // If max2 < max1, the last token must be placeholder "*"

    if (max2 > max1) return None

    if (max2 < max1) {
      if (max2 == 0) return None

      val lastToken = compiledPattern.last
      if (lastToken.isInstanceOf[DotRouteToken] ||
          !lastToken.asInstanceOf[NonDotRouteToken].isPlaceholder) return None
    }

    // Special case
    // 0 = max2 <= max1
    if (max2 == 0) {
      if (max1 == 0) return Some(MMap.empty[String, Seq[String]])
      return None
    }

    // 0 < max2 <= max1
    // pathParams is updated along the way
    val pathParams = MMap.empty[String, Seq[String]]
    val matched    = matchTokens(pathParams, pathTokens, compiledPattern)
    if (matched) Some(pathParams) else None
  }

  /** 0 < routeTokens.length <= pathTokens.length */
  @tailrec
  private def matchTokens(pathParams: Params, pathTokens: Seq[String], routeTokens: Seq[RouteToken]): Boolean = {
    val routeTokensLength = routeTokens.length
    val last              = routeTokensLength == 1
    val matched           = routeTokens.head.matchToken(pathParams, pathTokens, last)

    if (last) {
      matched
    } else {
      if (matched)
        matchTokens(pathParams, pathTokens.tail, routeTokens.tail)
      else
        false
    }
  }

  override def toString: String = {
    val withoutCacheInfo = httpMethod.name + " " + RouteCompiler.decompile(compiledPattern) + " -> " + klass.getName
    if (cacheSecs == 0)
      withoutCacheInfo
    else if (cacheSecs > 0)
      withoutCacheInfo + s" (page cache ${cacheSecs}s)"
    else
      withoutCacheInfo + s" (action cache ${-cacheSecs}s)"
  }
}
