package xt.vc.controller

import java.lang.reflect.Method
import java.util.{LinkedHashMap => JLinkedHashMap, List => JList}

import scala.collection.mutable.{ArrayBuffer, StringBuilder}

import org.jboss.netty.handler.codec.http.HttpMethod

import xt._
import xt.vc.env.{Env, PathInfo}

object Routes extends Logger {
  type KA              = (Class[Controller], Method)
  type Csas            = (String, String)
  type CompiledCsas    = (Csas, KA)
  type Pattern         = String
  type CompiledPattern = Array[(String, Boolean)]  // String: token, Boolean: true if the token is constant
  type Route           = (Option[HttpMethod], Pattern, KA)
  type CompiledRoute   = (Option[HttpMethod], CompiledPattern, KA, Csas)

  private var compiledRoutes: Iterable[CompiledRoute] = _

  def collectAndCompile {
    // Avoid loading twice in some servlet containers
    if (compiledRoutes != null) return

    val routes = (new RouteCollector).collect

    // Compile and log routes at the same time because the compiled routes do
    // not contain the original URL pattern.

    val patternMaxLength = routes.foldLeft(0) { (max, r) =>
      val len = r._2.length
      if (max < len) len else max
    }

    val builder = new StringBuilder
    builder.append("Routes:\n")
    compiledRoutes = routes.map { r =>
      val ret = compileRoute(r)

      val method = r._1 match {
        case None     => ""
        case Some(hm) => hm
      }
      val pattern = r._2.toString
      val controller = ret._4._1
      val action     = ret._4._2

      val format = "%-6s %-" + patternMaxLength + "s %s#%s\n"
      builder.append(format.format(method, pattern, controller, action))

      ret
    }
    logger.info(builder.toString)
  }

  /**
   * @return None if not matched or Some(pathParams)
   *
   * controller name and action name are put int pathParams.
   */
  def matchRoute(method: HttpMethod, pathInfo: PathInfo): Option[(KA, Env.Params)] = {
    val tokens = pathInfo.tokens

    val max1   = tokens.size

    var pathParams: Env.Params = null

    def finder(cr: CompiledRoute): Boolean = {
      val (om, compiledPattern, compiledCA, csas) = cr

      // Check methods
      if (om != None && om != Some(method)) return false

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
          pathParams = new JLinkedHashMap[String, JList[String]]()
          return true
        }

        return false
      }

      // 0 < max2 <= max1

      pathParams = new JLinkedHashMap[String, JList[String]]()
      var i = 0  // i will go from 0 until max1

      compiledPattern.forall { tc =>
        val (token, fixed) = tc

        val ret = if (fixed)
          (token == tokens(i))
        else {
          if (i == max2 - 1) {  // The last token
            if (token == "*") {
              val value = tokens.slice(i, max1).mkString("/")
              pathParams.put(token, Util.toValues(value))
              true
            } else {
              if (max2 < max1) {
                false
              } else {  // max2 = max1
                pathParams.put(token, Util.toValues(tokens(i)))
                true
              }
            }
          } else {
            if (token == "*") {
              false
            } else {
              pathParams.put(token, Util.toValues(tokens(i)))
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
        val (m, compiledPattern, compiledKA, csas) = cr
        val (cs, as) = csas
        pathParams.put("controller", Util.toValues(cs))
        pathParams.put("action",     Util.toValues(as))
        Some((compiledKA, pathParams))

      case None => None
    }
  }

  //----------------------------------------------------------------------------

  private def compileRoute(route: Route): CompiledRoute = {
    val (method, pattern, ka) = route
    val cp = compilePattern(pattern)
    (method, cp, ka, (ka._1.getName, ka._2.getName))
  }

  private def compilePattern(pattern: String): CompiledPattern = {
    val tokens = pattern.split("/").filter(_ != "")
    tokens.map { e: String =>
      val constant = !e.startsWith(":")
      val token    = if (constant) e else e.substring(1)
      (token, constant)
    }
  }
}
