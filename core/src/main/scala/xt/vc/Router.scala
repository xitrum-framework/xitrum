package xt.vc

import xt.{Config, Logger, URLDecoder}

import java.lang.reflect.Method

import scala.collection.mutable.{ArrayBuffer, StringBuilder}

import org.reflections.Reflections
import org.reflections.util.{ConfigurationBuilder, ClasspathHelper}

import org.jboss.netty.handler.codec.http.HttpMethod

object Router extends Logger {
  type KA              = (Class[Controller], Method)
  type Csas            = (String, String)
  type CompiledCsas    = (Csas, KA)
  type Pattern         = String
  type CompiledPattern = Array[(String, Boolean)]  // String: token, Boolean: true if the token is constant
  type Route           = (Option[HttpMethod], Pattern, KA)
  type CompiledRoute   = (Option[HttpMethod], CompiledPattern, KA, Csas)

  private var compiledRoutes: Iterable[CompiledRoute] = _


  def collectAndCompile {
    val routes = collectRoutes

    // Compile and log routes at the same time because the compiled routes do
    // not contain the original URL pattern.

    val patternMaxLength = routes.foldLeft(0) { (max, r) =>
      val len = r._2.length
      if (max < len) len else max
    }

    val routesDebugString = new StringBuilder
    routesDebugString.append("Routes:\n")
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
      routesDebugString.append(format.format(method, pattern, controller, action))

      ret
    }
    logger.debug(routesDebugString.toString)

    compiledRoutes
  }

  /**
   * @return None if not matched or Some(routeParams)
   *
   * controller name and action name are put int routeParams.
   */
  def matchRoute(method: HttpMethod, pathInfo: String): Option[(KA, Env.Params)] = {
    val tokens = pathInfo.split("/").filter(_ != "")
    val max1   = tokens.size

    var routeParams: Env.Params = null

    val finder = (cr: CompiledRoute) => {
      val (om, compiledPattern, csas, compiledCA) = cr

      // Must be <= max1
      // If < max1, the last token must be "*" and non-fixed
      val max2 = compiledPattern.size

      if (max2 > max1 || (om != None && om != Some(method)))
        false
      else {
        if (max2 == 0) {
          if (max1 == 0) {
            routeParams = new java.util.LinkedHashMap[String, java.util.List[String]]()
            true
          } else
            false
        } else {
          routeParams = new java.util.LinkedHashMap[String, java.util.List[String]]()
          var i = 0  // i will go from 0 until max1

          compiledPattern.forall { tc =>
            val (token, constant) = tc
            val ret = if (constant)
              (token == tokens(i))
            else {
              if (i == max2 - 1) {
                if (token == "*") {
                  val value = tokens.slice(i, max1).mkString("/")
                  routeParams.put(token, toValues(value))
                  true
                } else {
                  if (max2 < max1) {
                    false
                  } else {  // max2 = max1
                    val value = tokens(i)
                    routeParams.put(token, toValues(value))
                    true
                  }
                }
              } else {  // Not the last token
                if (token == "*") {
                  false
                } else {
                  URLDecoder.decode(tokens(i)) match {
                    case None => false

                    case Some(value) =>
                      routeParams.put(token, toValues(value))
                      true
                  }
                }
              }
            }

            i += 1
            ret
          }
        }
      }
    }

    compiledRoutes.find(finder) match {
      case Some(cr) =>
        val (m, compiledPattern, compiledKA, csas) = cr
        val (cs, as) = csas
        routeParams.put("controller", toValues(cs))
        routeParams.put("action",     toValues(as))
        Some((compiledKA, routeParams))

      case None => None
    }
  }

  /**
   * WARN: This method is here because it is also used by Failsafe when redispatching.
   *
   * Wraps a single String by a List.
   */
  def toValues(value: String): java.util.List[String] = {
    val values = new java.util.ArrayList[String](1)
    values.add(value)
    values
  }

  //----------------------------------------------------------------------------

  /** Scan all subtypes of class Controller to collect routes. */
  def collectRoutes: Array[Route] = {
    val cb = new ConfigurationBuilder
    cb.setUrls(ClasspathHelper.getUrlsForCurrentClasspath)
    val r = new Reflections(cb)

    val ks = r.getSubTypesOf(classOf[Controller])  // Controller classes
    val ik = ks.iterator
    val routes = ArrayBuffer[Route]()
    while (ik.hasNext) {
      val k = ik.next.asInstanceOf[Class[Controller]]
      val pathPrefix = {
        val pathAnnotation = k.getAnnotation(classOf[Path])
        if (pathAnnotation != null) pathAnnotation.value else ""
      }

      val ms = k.getMethods                        // Methods
      for (m <- ms) {
        val as = m.getAnnotations
        val paths = ArrayBuffer[String]()
        val httpMethods = ArrayBuffer[Option[HttpMethod]]()
        for (a <- as) {
          if (a.isInstanceOf[Path]) {
            paths.append(pathPrefix + a.asInstanceOf[Path].value)
          } else if (a.isInstanceOf[GET]) {
            httpMethods.append(Some(HttpMethod.GET))
          } else if (a.isInstanceOf[POST]) {
            httpMethods.append(Some(HttpMethod.POST))
          } else if (a.isInstanceOf[PUT]) {
            httpMethods.append(Some(HttpMethod.PUT))
          } else if (a.isInstanceOf[DELETE]) {
            httpMethods.append(Some(HttpMethod.DELETE))
          }
        }

        if (!paths.isEmpty) {  // m is an action
          if (httpMethods.isEmpty) httpMethods.append(None)

          for (p <- paths; hm <- httpMethods) routes.append((hm, p, (k, m)))
        }
      }
    }

    routes.toArray
  }

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

  // Same as Rails' config.filter_parameters
  private def filterParams(params: java.util.Map[String, java.util.List[String]]): java.util.Map[String, java.util.List[String]] = {
    val ret = new java.util.LinkedHashMap[String, java.util.List[String]]()
    ret.putAll(params)
    for (key <- Config.filterParams) {
      if (ret.containsKey(key)) ret.put(key, toValues("[filtered]"))
    }
    ret
  }
}
