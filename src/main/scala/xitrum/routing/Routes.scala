package xitrum.routing

import java.lang.reflect.Method
import scala.collection.mutable.{ArrayBuffer, Map => MMap, StringBuilder}
import io.netty.handler.codec.http.{HttpMethod, QueryStringEncoder}

import xitrum.{Controller, Config, Logger}
import xitrum.scope.request.{Params, PathInfo}

object Routes extends Logger with ActionPageCacheApi with RouteApi {
  type First_Other_Last = (ArrayBuffer[Route], ArrayBuffer[Route], ArrayBuffer[Route])

  /**
   * Route matching: httpMethod -> order -> pattern
   * When matched, routeMethod is used for creating a new controller instance,
   * then routeMethod is invoked on that instance.
   */
  val routes = MMap[HttpMethod, First_Other_Last]()

  /** 404.html and 500.html is used by default */
  var route404: Route = _
  var route500: Route = _

  //----------------------------------------------------------------------------

  def compilePattern(pattern: String): CompiledPattern = {
    val tokens = pattern.split('/').filter(_ != "")
    tokens.map { e: String =>
      val constant = !e.startsWith(":")
      val token = if (constant) e else e.substring(1)
      (token, constant)
    }
  }

  def decompiledPattern(compiledPattern: CompiledPattern): String = {
    if (compiledPattern.isEmpty) {
      "/"
    } else {
      compiledPattern.foldLeft("") { (acc, tc) =>
        val (token, isConstant) = tc
        val rawToken = if (isConstant) token else ":" + token
        acc + "/" + rawToken
      }
    }
  }

  //----------------------------------------------------------------------------

  def printRoutes() {
    logger.info("Routes:\n" + routes)

    for ((httpMethod, (firsts, others, lasts)) <- routes) {
      println(httpMethod)
      for (route <- firsts ++ others ++ lasts) {
      val pattern = decompiledPattern(route.compiledPattern)
        println(pattern + " " + ControllerReflection.fullFriendlyActionName(route))
      }
    }
  }

  def printCaches() {
    logger.info("Caches: FIXME")
  }

  //----------------------------------------------------------------------------

  def fromCacheFileOrRecollect() {
    val routeCollector                    = new RouteCollector("routes.sclasner")
    val routeMethodsGroupedByControllers = routeCollector.fromCacheFileOrRecollect()

    routeMethodsGroupedByControllers.foreach { routeMethods =>
      val controllerClass = routeMethods.head.getDeclaringClass
      val controller      = controllerClass.newInstance()
      routeMethods.foreach { routeMethod =>
        val route = routeMethod.invoke(controller).asInstanceOf[Route]
        if (route.body != null) {
          val withRouteMethod = Route(route.httpMethod, route.order, route.compiledPattern, route.body, routeMethod, route.cacheSeconds)

          val firsts_others_lasts =
            if (routes.isDefinedAt(route.httpMethod)) {
              routes(route.httpMethod)
            } else {
              val ret = (ArrayBuffer[Route](), ArrayBuffer[Route](), ArrayBuffer[Route]())
              routes(route.httpMethod) = ret
              ret
            }

          val arrayBuffer = route.order match {
            case RouteOrder.FIRST => firsts_others_lasts._1
            case RouteOrder.OTHER => firsts_others_lasts._2
            case RouteOrder.LAST  => firsts_others_lasts._3
          }

          arrayBuffer.append(withRouteMethod)
        }
      }
    }
  }

  def lookupRouteMethod(route: Route): Method = null  // FIXME

  /** For use from browser */
  lazy val jsRoutes = {
    "FIXME"
    /*
    val xs = compiledRoutes.map { case (_, compiledPattern, routeMethod) =>
      val ys = compiledPattern.map { case (token, constant) =>
        "['" + token + "', " + constant + "]"
      }
      "[[" + ys.mkString(", ") + "], '" + routeMethod.getName + "']"
    }
    "[" + xs.mkString(", ") + "]"
    */
  }

  //----------------------------------------------------------------------------

  def matchRoute(httpMethod: HttpMethod, pathInfo: PathInfo): Option[(Route, Params)] = {
    if (!routes.isDefinedAt(httpMethod)) return None

    val tokens = pathInfo.tokens
    val max1 = tokens.size

    var pathParams: Params = null

    def finder(route: Route): Boolean = {
      val compiledPattern = route.compiledPattern

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
      var i = 0 // i will go from 0 until max1

      compiledPattern.forall { tc =>
        val (token, fixed) = tc

        val ret = if (fixed)
          (token == tokens(i))
        else {
          if (i == max2 - 1) { // The last token
            if (token == "*") {
              val value = tokens.slice(i, max1).mkString("/")
              pathParams(token) = List(value)
              true
            } else {
              if (max2 < max1) {
                false
              } else { // max2 = max1
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

    val (firsts, others, lasts) = routes(httpMethod)
    (firsts ++ others ++ lasts).find(finder) match {
      case Some(route) =>
        Some((route, pathParams))

      case None => None
    }
  }
}
