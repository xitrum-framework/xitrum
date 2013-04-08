package xitrum.routing

import scala.collection.mutable.{Map => MMap}
import org.jboss.netty.handler.codec.http.{HttpMethod, QueryStringEncoder}

import xitrum.{Config, ActionEnv}
import xitrum.scope.request.Params

/** @param cacheSecs < 0: cache action, > 0: cache page, 0: no cache */
class Route(
  // In
  val httpMethod: HttpMethod, val compiledPattern: Seq[RouteToken],

  // Out
  val actionClass: Class[ActionEnv], val cacheSecs: Int
)
{
  def url(params: (String, Any)*) = {
    var map = params.toMap
    val tokens = compiledPattern.map { rt =>
      if (rt.isPlaceHolder) {
        val key = rt.value
        if (!map.isDefinedAt(key))
          throw new Exception("Cannot compute reverse URL because there's no required key \"" + key + "\"")

        val ret = map(key)
        map = map - key
        ret
      } else {
        rt.value
      }
    }
    val url = Config.withBaseUrl("/" + tokens.mkString("/"))

    val qse = new QueryStringEncoder(url, Config.requestCharset)
    for ((k, v) <- map) qse.addParam(k, v.toString)
    qse.toString
  }

  /** @return None if not matched */
  def matchRoute(pathTokens: Array[String]): Option[Params] = {
    val max1 = pathTokens.size
    val max2 = compiledPattern.size

    // Check the number of tokens
    // max2 must be <= max1
    // If max2 < max1, the last token must be "*" and non-fixed

    if (max2 > max1) return None

    if (max2 < max1) {
      if (max2 == 0) return None

      val lastToken = compiledPattern.last
      if (!lastToken.isPlaceHolder) return None
    }

    // Special case
    // 0 = max2 <= max1
    if (max2 == 0) {
      if (max1 == 0) return Some(MMap[String, Seq[String]]())
      return None
    }

    // 0 < max2 <= max1

    val pathParams = MMap[String, Seq[String]]()
    var i = 0   // i will go from 0 until max1

    // pathParams is updated along the way
    val matched = compiledPattern.forall { rt =>
      val ret = if (rt.isPlaceHolder) {
        if (i == max2 - 1) {  // The last token
          if (rt.value == "*") {
            val value = pathTokens.slice(i, max1).mkString("/")
            matchRegex(pathParams, rt, value)
          } else {
            if (max2 < max1) {
              false
            } else { // max2 = max1
              matchRegex(pathParams, rt, pathTokens(i))
            }
          }
        } else {
          if (rt.value == "*") {
            false
          } else {
            matchRegex(pathParams, rt, pathTokens(i))
          }
        }
      } else {
        (rt.value == pathTokens(i))
      }

      i += 1
      ret
    }

    if (matched) Some(pathParams) else None
  }

  /** pathParams is updated */
  private def matchRegex(pathParams: Params, rt: RouteToken, value: String): Boolean = {
    rt.regex match {
      case None =>
        pathParams(rt.value) = Seq(value)
        true

      case Some(r) =>
        r.findFirstIn(value) match {
          case None => false
          case _ =>
            pathParams(rt.value) = Seq(value)
            true
        }
    }
  }
}
