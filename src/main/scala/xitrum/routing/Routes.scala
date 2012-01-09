package xitrum.routing

import java.io.File
import java.lang.reflect.Method
import scala.collection.mutable.{ArrayBuffer, Map => MMap, StringBuilder}
import io.netty.handler.codec.http.{HttpMethod, QueryStringEncoder}

import xitrum.{Config, Logger}
import xitrum.controller.Action
import xitrum.scope.request.{Params, PathInfo}

object Routes extends Logger {
  type First_Other_Last = (ArrayBuffer[Action], ArrayBuffer[Action], ArrayBuffer[Action])

  /**
   * Route matching: httpMethod -> order -> pattern
   * When matched, routeMethod is used for creating a new controller instance,
   * then routeMethod is invoked on that instance.
   */
  val actions = MMap[HttpMethod, First_Other_Last]()

  /** 404.html and 500.html is used by default */
  var error404: Action = _
  var error500: Action = _

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
    // This method is only run once on start, speed is not a problem

    val firsts = ArrayBuffer[(String, String, String)]()
    var others = ArrayBuffer[(String, String, String)]()
    val lasts  = ArrayBuffer[(String, String, String)]()
    for ((httpMethod, (fs, os, ls)) <- actions) {
      for (a <- fs) firsts.append((httpMethod.toString, decompiledPattern(a.route.compiledPattern), ControllerReflection.controllerActionName(a)))
      for (a <- os) others.append((httpMethod.toString, decompiledPattern(a.route.compiledPattern), ControllerReflection.controllerActionName(a)))
      for (a <- ls) lasts.append ((httpMethod.toString, decompiledPattern(a.route.compiledPattern), ControllerReflection.controllerActionName(a)))
    }

    var all = firsts ++ others ++ lasts
    val (methodHttpMaxLength, patternMaxLength) = all.foldLeft((0, 0)) { case ((mmax, pmax), (m, p, _)) =>
      val mlen  = m.length
      val plen  = p.length
      val mmax2 = if (mmax < mlen) mlen else mmax
      val pmax2 = if (pmax < plen) plen else pmax
      (mmax2, pmax2)
    }
    val logFormat = "%-" + methodHttpMaxLength + "s    %-" + patternMaxLength + "s    %s"

    others = others.sortBy(_._3)
    all = firsts ++ others ++ lasts

    val strings = all.map { case (m, p, cr) => logFormat.format(m, p, cr) }
    logger.info("Route:\n" + strings.mkString("\n"))
  }

  def printActionPageCaches() {
    // This method is only run once on start, speed is not a problem

    var actionCaches = ArrayBuffer[(String, Int)]()
    var actionMaxControllerActionNameLength = 0

    var pageCaches = ArrayBuffer[(String, Int)]()
    var pageMaxControllerActionNameLength = 0

    for ((httpMethod, (fs, os, ls)) <- actions) {
      val all = fs ++ os ++ ls
      for (a <- all) {
        if (a.cacheSeconds < 0) {
          val n = ControllerReflection.controllerActionName(a)
          actionCaches.append((n, -a.cacheSeconds))

          val nLength = n.length
          if (nLength > actionMaxControllerActionNameLength) actionMaxControllerActionNameLength = nLength
        } else if (a.cacheSeconds > 0) {
          val n = ControllerReflection.controllerActionName(a)
          pageCaches.append((n, a.cacheSeconds))

          val nLength = n.length
          if (nLength > pageMaxControllerActionNameLength) pageMaxControllerActionNameLength = nLength
        }
      }
    }

    def formatTime(seconds: Int): String = {
      if (seconds < 60) {
        "%d [sec]".format(seconds)
      } else {
        val minutes = seconds / 60
        if (minutes < 60) {
          "%d [min]".format(minutes)
        } else {
          val hours = minutes / 60
          if (hours < 24) {
            "%d [h]".format(hours)
          } else {
            val days = hours / 24
            "%d [d]".format(days)
          }
        }
      }
    }

    if (!actionCaches.isEmpty) {
      actionCaches = actionCaches.sortBy(_._1)
      val logFormat = "%-" + actionMaxControllerActionNameLength + "s    %s"
      val strings = actionCaches.map { case (n, s) => logFormat.format(n, formatTime(s)) }
      logger.info("Action cache:\n" + strings.mkString("\n"))
    }

    if (!pageCaches.isEmpty) {
      pageCaches = pageCaches.sortBy(_._1)
      val logFormat = "%-" + pageMaxControllerActionNameLength + "s    %s"
      val strings = pageCaches.map { case (n, s) => logFormat.format(n, formatTime(s)) }
      logger.info("Page cache:\n" + strings.mkString("\n"))
    }
  }

  //----------------------------------------------------------------------------

  def fromCacheFileOrRecollect() {
    fromCacheFileOrRecollectWithRetry("routes.sclasner")
  }

  private def fromCacheFileOrRecollectWithRetry(cachedFileName: String) {
    try {
      logger.info("Load " + cachedFileName + "/collect routes and action/page cache config from controllers...")
      fromCacheFileOrRecollectReal(cachedFileName)
    } catch {
      case e =>
        // Maybe routes.sclasner could not be loaded because dependencies have changed.
        // Try deleting routes.sclasner and scan again.
        val f = new File(cachedFileName)
        if (f.exists) {
          logger.warn("Error loading " + cachedFileName + ". Delete the file and recollect...")
          f.delete()
          try {
            fromCacheFileOrRecollectReal("routes.sclasner")
          } catch {
            case e2 =>
              Config.exitOnError("Could not collect routes", e2)
              throw e2
          }
        } else {
          Config.exitOnError("Could not collect routes", e)
          throw e
        }
    }
  }

  private def fromCacheFileOrRecollectReal(cachedFileName: String) {
    val routeCollector                          = new RouteCollector(cachedFileName)
    val controllerClassName_to_routeMethodNames = routeCollector.fromCacheFileOrRecollect()

    for ((controllerClassName, routeMethodNames) <- controllerClassName_to_routeMethodNames) {
      for (routeMethodName <- routeMethodNames) {
        ControllerReflection.getActionMethod(controllerClassName, routeMethodName) match {
          case None =>
          case Some(actionMethod) =>
            val controllerClass = actionMethod.getDeclaringClass
            val controller      = controllerClass.newInstance()
            val action          = actionMethod.invoke(controller).asInstanceOf[Action]
            if (action.route != null && action.route.httpMethod != null) {  // Actions created by indirectAction do not have route
              action.method = actionMethod  // Cache it

              val firsts_others_lasts =
                if (actions.isDefinedAt(action.route.httpMethod)) {
                  actions(action.route.httpMethod)
                } else {
                  val ret = (ArrayBuffer[Action](), ArrayBuffer[Action](), ArrayBuffer[Action]())
                  actions(action.route.httpMethod) = ret
                  ret
                }

              val arrayBuffer = action.route.order match {
                case RouteOrder.FIRST => firsts_others_lasts._1
                case RouteOrder.OTHER => firsts_others_lasts._2
                case RouteOrder.LAST  => firsts_others_lasts._3
              }

              arrayBuffer.append(action)
            }
        }
      }
    }
  }

  /** For use from browser */
  lazy val jsRoutes = {
    val actionArray = ArrayBuffer[Action]()
    for ((httpMethod, (firsts, others, lasts)) <- actions) {
      val all = firsts ++ others ++ lasts
      actionArray.appendAll(all)
    }

    val xs = actionArray.map { action =>
      val ys = action.route.compiledPattern.map { case (token, constant) =>
        "['" + token + "', " + constant + "]"
      }
      "[[" + ys.mkString(", ") + "], '" + ControllerReflection.controllerActionName(action) + "']"
    }
    "[" + xs.mkString(", ") + "]"
  }

  //----------------------------------------------------------------------------

  def matchRoute(httpMethod: HttpMethod, pathInfo: PathInfo): Option[(Action, Params)] = {
    // This method is only run for every request, speed is a problem

    if (!actions.isDefinedAt(httpMethod)) return None

    val tokens = pathInfo.tokens
    val max1   = tokens.size

    var pathParams: Params = null

    def finder(action: Action): Boolean = {
      val compiledPattern = action.route.compiledPattern
      val max2            = compiledPattern.size

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

      compiledPattern.forall { case (token, fixed) =>
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

    actions.get(httpMethod) match {
      case None => None
      case Some((firsts, others, lasts)) =>
        firsts.find(finder) match {
          case Some(action) => Some((action, pathParams))
          case None =>
            others.find(finder) match {
              case Some(action) => Some((action, pathParams))
              case None =>
                lasts.find(finder) match {
                  case Some(action) => Some((action, pathParams))
                  case None => None
                }
            }
        }
    }
  }
}
