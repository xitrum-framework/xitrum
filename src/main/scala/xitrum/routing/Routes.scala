package xitrum.routing

import java.lang.reflect.Method
import scala.collection.mutable.{ArrayBuffer, Map => MMap, StringBuilder}
import org.jboss.netty.handler.codec.http.{HttpMethod, QueryStringEncoder}

import xitrum.{Action, Config, Logger}
import xitrum.scope.request.{Params, PathInfo}

object Routes extends Logger {
  type Pattern         = String
  type CompiledPattern = Array[(String, Boolean)]  // String: token, Boolean: true if the token is constant
  type Route           = (HttpMethod, Pattern,         Class[Action])
  type CompiledRoute   = (HttpMethod, CompiledPattern, Class[Action])

  private var compiledRoutes: Iterable[CompiledRoute] = _
  private var cacheSecs:      Map[Class[Action], Int] = _  // Int: 0 = no cache, < 0 = action, > 0 = page

  def collectAndCompile {
    // Avoid loading twice in some servlet containers
    if (compiledRoutes != null) return

    val (routes, cacheSecs0) = (new RouteCollector).collect
    cacheSecs = cacheSecs0

    // Compile and log routes
    // The compiled routes do not contain the original URL pattern

    val (methodMaxLength, patternMaxLength) = routes.foldLeft((0, 0)) { case ((mmax, pmax), (method, pattern, actionClass)) =>
      val mlen = method.getName.length
      val plen = pattern.length

      // actionClass != classOf[PostbackAction]: see below
      val mmax2 = if (actionClass != classOf[PostbackAction] && mmax < mlen) mlen else mmax
      val pmax2 = if (pmax < plen) plen else pmax
      (mmax2, pmax2)
    }
    val logFormat = "%-" + methodMaxLength + "s %-" + patternMaxLength + "s %s\n"

    val builder = new StringBuilder
    builder.append("Routes:\n")
    compiledRoutes = routes.map { r =>
      val method      = r._1
      val pattern     = r._2
      val actionClass = r._3

      if (actionClass != classOf[PostbackAction])  // Skip noisy information
        builder.append(logFormat.format(method.getName, pattern, actionClass.getName))

      compileRoute(r)
    }
    logger.info(builder.toString)
  }

  lazy val jsRoutes = {
    val xs = compiledRoutes.map { case (_, compiledPattern, actionClass) =>
      val ys = compiledPattern.map { case (token, constant) =>
        "['" + token + "', " + constant + "]"
      }

      "[[" + ys.mkString(", ") + "], '" + actionClass.getName + "']"
    }

    "[" + xs.mkString(", ") + "]"
  }

  /**
   * This method may specify a new HttpMethod to override the old method.
   *
   * @return None if not matched
   */
  def matchRoute(method: HttpMethod, pathInfo: PathInfo): Option[(HttpMethod, Class[Action], Params)] = {
    val tokens = pathInfo.tokens
    val max1   = tokens.size

    var pathParams: Params = null

    def finder(cr: CompiledRoute): Boolean = {
      val (om, compiledPattern, _actionClass) = cr

      // Check method
      if (om != method) return false

      val max2 = compiledPattern.size

      // Check the number of tokens
      // max2 must be <= max1
      // If max2 < max1, the last token must be "*" and non-fixed

      if (max2 > max1) return false

      if (max2 < max1) {
        if (max2 == 0) return false

        val lastToken = compiledPattern.last
        if (lastToken._2) return false
      }

      // Special case
      // 0 = max2 <= max1
      if (max2 == 0) {
        if (max1 == 0) {
          pathParams = MMap[String, List[String]]()
          return true
        }

        return false
      }

      // 0 < max2 <= max1

      pathParams = MMap[String, List[String]]()
      var i = 0  // i will go from 0 until max1

      compiledPattern.forall { tc =>
        val (token, fixed) = tc

        val ret = if (fixed)
          (token == tokens(i))
        else {
          if (i == max2 - 1) {  // The last token
            if (token == "*") {
              val value = tokens.slice(i, max1).mkString("/")
              pathParams(token) = List(value)
              true
            } else {
              if (max2 < max1) {
                false
              } else {  // max2 = max1
                pathParams(token) = List(tokens(i))
                true
              }
            }
          } else {
            if (token == "*") {
              false
            } else {
              pathParams(token) = List(tokens(i))
              true
            }
          }
        }

        i += 1
        ret
      }
    }

    compiledRoutes.find(finder) match {
      case Some(cr) =>
        val (_m, _compiledPattern, actionClass) = cr
        Some((method, actionClass, pathParams))

      case None => None
    }
  }

  def getCacheSecs(actionClass: Class[Action]) = cacheSecs.getOrElse(actionClass, 0)

  def urlFor(actionClass: Class[Action], params: (String, Any)*): String = {
    val cpo = compiledRoutes.find { case (_, _, klass) => klass == actionClass }
    if (cpo.isEmpty) {
      throw new Exception("Missing route for urlFor: " + actionClass.getName)
    } else {
      val compiledPattern = cpo.get._2
      urlForNonPostback(compiledPattern, params:_*)
    }
  }

  //----------------------------------------------------------------------------

  private def compileRoute(route: Route): CompiledRoute = {
    val (method, pattern, actionClass) = route
    val cp = compilePattern(pattern)
    (method, cp, actionClass)
  }

  private def compilePattern(pattern: String): CompiledPattern = {
    val tokens = pattern.split('/').filter(_ != "")
    tokens.map { e: String =>
      val constant = !e.startsWith(":")
      val token    = if (constant) e else e.substring(1)
      (token, constant)
    }
  }

  private def urlForNonPostback(compiledPattern: CompiledPattern, params: (String, Any)*): String = {
    var map = params.toMap
    val tokens = compiledPattern.map { case (token, constant) =>
      if (constant) {
        token
      } else {
        val ret = map(token)
        map = map - token
        ret
      }
    }
    val url = Config.withBaseUri("/" + tokens.mkString("/"))

    val qse = new QueryStringEncoder(url, Config.requestCharset)
    for ((k, v) <- map) qse.addParam(k, v.toString)
    qse.toString
  }
}
