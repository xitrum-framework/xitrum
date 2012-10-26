package xitrum.routing

import java.io.File
import java.lang.reflect.Method

import scala.collection.mutable.{ArrayBuffer, Map => MMap, StringBuilder}
import scala.util.matching.Regex

import org.apache.commons.lang3.ClassUtils
import org.jboss.netty.handler.codec.http.{HttpMethod, QueryStringEncoder}

import xitrum.{Config, Controller, ErrorController, Logger, SockJsHandler}
import xitrum.controller.Action
import xitrum.scope.request.{Params, PathInfo}
import xitrum.sockjs.SockJsController

object Routes extends Logger {
  type First_Other_Last = (ArrayBuffer[Action], ArrayBuffer[Action], ArrayBuffer[Action])

  private val ROUTES_CACHE = "routes.cache"

  /**
   * Route matching: httpMethod -> order -> pattern
   * When matched, method is used for creating a new controller instance,
   * then the method is invoked on that instance to get the action.
   */
  private val actions = MMap[HttpMethod, First_Other_Last]()

  /** 404.html and 500.html is used by default */
  var error: Class[_ <: ErrorController] = _

  //----------------------------------------------------------------------------

  def printRoutes() {
    // This method is only run once on start, speed is not a problem

    val firsts = ArrayBuffer[(String, String, String)]()
    var others = ArrayBuffer[(String, String, String)]()
    val lasts  = ArrayBuffer[(String, String, String)]()
    for ((httpMethod, (fs, os, ls)) <- actions) {
      for (a <- fs) firsts.append((httpMethod.toString, RouteCompiler.decompile(a.route.compiledPattern), ControllerReflection.controllerActionName(a)))
      for (a <- os) others.append((httpMethod.toString, RouteCompiler.decompile(a.route.compiledPattern), ControllerReflection.controllerActionName(a)))
      for (a <- ls) lasts.append ((httpMethod.toString, RouteCompiler.decompile(a.route.compiledPattern), ControllerReflection.controllerActionName(a)))
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
    logger.info("Routes:\n" + strings.mkString("\n"))
  }

  def printSockJsRoutes() {
    if (!sockJsRoutes.isEmpty) {
      val strings = ArrayBuffer[String]()
      for ((pathPrefix, handlerClass) <- sockJsRoutes) {
        strings += pathPrefix + " -> " + handlerClass.getName
      }
      logger.info("SockJS routes:\n" + strings.mkString("\n"))
    }
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

    if (actionCaches.nonEmpty) {
      actionCaches = actionCaches.sortBy(_._1)
      val logFormat = "%-" + actionMaxControllerActionNameLength + "s    %s"
      val strings = actionCaches.map { case (n, s) => logFormat.format(n, formatTime(s)) }
      logger.info("Action cache:\n" + strings.mkString("\n"))
    }

    if (pageCaches.nonEmpty) {
      pageCaches = pageCaches.sortBy(_._1)
      val logFormat = "%-" + pageMaxControllerActionNameLength + "s    %s"
      val strings = pageCaches.map { case (n, s) => logFormat.format(n, formatTime(s)) }
      logger.info("Page cache:\n" + strings.mkString("\n"))
    }
  }

  def fromCacheFileOrRecollect() {
    // Avoid running twice, older version of Xitrum (v1.8) needs apps to
    // call this method explicitly
    if (actions.isEmpty) fromCacheFileOrRecollectWithRetry()
  }

  def fromSockJsController() {
    val routeCollector                        = new RouteCollector
    val controllerClassName_actionMethodNames = routeCollector.fromSockJsController
    fromControllerClassName_ActionMethodNames(controllerClassName_actionMethodNames, true)
  }

  //----------------------------------------------------------------------------

  private def fromCacheFileOrRecollectWithRetry() {
    try {
      logger.info("Load " + ROUTES_CACHE + "/collect routes and action/page cache config from controllers...")
      fromCacheFileOrRecollectReal()
    } catch {
      case e =>
        // Maybe ROUTES_CACHE file could not be loaded because dependencies have changed.
        // Try deleting and scanning again.
        val f = new File(ROUTES_CACHE)
        if (f.exists) {
          logger.warn("Error loading " + ROUTES_CACHE + ". Delete the file and recollect...")
          f.delete()
          try {
            actions.clear()  // Reset partly-collected routes
            fromCacheFileOrRecollectReal()
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

  private def fromCacheFileOrRecollectReal() {
    val routeCollector                        = new RouteCollector
    val controllerClassName_actionMethodNames = routeCollector.fromCacheFileOrRecollect(ROUTES_CACHE)
    fromControllerClassName_ActionMethodNames(controllerClassName_actionMethodNames, false)
  }

  private def fromControllerClassName_ActionMethodNames(controllerClassName_actionMethodNames: Map[String, Seq[String]], forSockJsController: Boolean) {
    for ((controllerClassName, actionMethodNames) <- controllerClassName_actionMethodNames) {
      for (actionMethodName <- actionMethodNames) {
        getActionMethod(controllerClassName, actionMethodName) match {
          case None =>

          case Some(actionMethod) =>
            val controllerClass = actionMethod.getDeclaringClass
            val controller      = controllerClass.newInstance().asInstanceOf[Controller]

            if (forSockJsController) {
              for (pathPrefix <- sockJsRoutes.keys) {
                // "first" and "last" can't be "lazy val" because pathPrefix is
                // reset here
                controller.pathPrefix = pathPrefix
                populateActions(controller, actionMethod)
              }
            } else {
              populateActions(controller, actionMethod)
            }
        }
      }
    }
  }

  private def populateActions(controller: Controller, actionMethod: Method) {
    val action = actionMethod.invoke(controller).asInstanceOf[Action]
    // Actions created by indirectAction do not have route
    if (action.route != null && action.route.httpMethod != null) {
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

  private def getActionMethod(className: String, methodName: String): Option[Method] = {
    val klass = Class.forName(className)
    if (classOf[Controller].isAssignableFrom(klass)) {  // Should be a subclass of Controller
      // Should be "def", not "val"
      try {
        klass.getDeclaredField(methodName)
        None
      } catch {
        case _ =>  // NoSuchFieldException
          val actionMethod = klass.getMethod(methodName)
          Some(actionMethod)
      }
    } else {
      None
    }
  }

  //----------------------------------------------------------------------------

  def lookupMethod(route: Route): Method = {
    val (firsts, others, lasts) = actions(route.httpMethod)
    firsts.foreach { a => if (a.route == route) return a.method }
    others.foreach { a => if (a.route == route) return a.method }
    lasts .foreach { a => if (a.route == route) return a.method }
    throw new Exception("Route not found: " + route)
  }

  /** For use from browser */
  lazy val jsRoutes = {
    val actionArray = ArrayBuffer[Action]()
    for ((httpMethod, (firsts, others, lasts)) <- actions) {
      val all = firsts ++ others ++ lasts
      actionArray.appendAll(all)
    }

    val xs = actionArray.map { action =>
      val ys = action.route.compiledPattern.map { rt =>
        "['" + rt.value + "', " + rt.isPlaceHolder + "]"
      }
      "[[" + ys.mkString(", ") + "], '" + ControllerReflection.controllerActionName(action) + "']"
    }
    "[" + xs.mkString(", ") + "]"
  }

  //----------------------------------------------------------------------------

  def matchRoute(httpMethod: HttpMethod, pathInfo: PathInfo): Option[(Method, Params)] = {
    // This method is only run for every request, speed is a problem

    if (!actions.isDefinedAt(httpMethod)) return None

    val tokens = pathInfo.tokens
    val max1   = tokens.size

    var pathParams: Params = null

    def finder(action: Action): Boolean = {
      val routeTokens = action.route.compiledPattern
      val max2        = routeTokens.size

      // Check the number of tokens
      // max2 must be <= max1
      // If max2 < max1, the last token must be "*" and non-fixed

      if (max2 > max1) return false

      if (max2 < max1) {
        if (max2 == 0) return false

        val lastToken = routeTokens.last
        if (!lastToken.isPlaceHolder) return false
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

      def matchRegex(rt: RouteToken, value: String): Boolean = {
        rt.regex match {
          case None =>
            pathParams(rt.value) = List(value)
            true
          case Some(r) =>
            r.findFirstIn(value) match {
              case None =>
                false
              case _ =>
                pathParams(rt.value) = List(value)
                true
            }
        }
      }

      routeTokens.forall { rt =>
        val ret = if (rt.isPlaceHolder) {
          if (i == max2 - 1) { // The last token
            if (rt.value == "*") {
              val value = tokens.slice(i, max1).mkString("/")
              matchRegex(rt, value)
            } else {
              if (max2 < max1) {
                false
              } else { // max2 = max1
                matchRegex(rt, tokens(i))
              }
            }
          } else {
            if (rt.value == "*") {
              false
            } else {
              matchRegex(rt, tokens(i))
            }
          }
        } else {
          (rt.value == tokens(i))
        }

        i += 1
        ret
      }
    }

    // actions.isDefinedAt(httpMethod) has been checked above
    val (firsts, others, lasts) = actions(httpMethod)
    firsts.find(finder) match {
      case Some(action) => Some((action.method, pathParams))
      case None =>
        others.find(finder) match {
          case Some(action) => Some((action.method, pathParams))
          case None =>
            lasts.find(finder) match {
              case Some(action) => Some((action.method, pathParams))
              case None => None
            }
        }
    }
  }

  lazy val action404Method: Option[Method] = Option(error).map(_.getMethod("error404"))

  lazy val action500Method: Option[Method] = Option(error).map(_.getMethod("error500"))

  //----------------------------------------------------------------------------

  private val sockJsRoutes = MMap[String, Class[_ <: SockJsHandler]]()

  def sockJs(SockJsHandlerClass: Class[_ <: SockJsHandler], pathPrefix: String) {
    sockJsRoutes(pathPrefix) = SockJsHandlerClass
  }

  def createSockJsHandler(pathPrefix: String): SockJsHandler = {
    val klass = sockJsRoutes(pathPrefix)
    klass.newInstance()
  }

  /** @param sockJsHandlerClass Normal SockJsHandler subclass or object class */
  def sockJsPathPrefix(sockJsHandlerClass: Class[_ <: SockJsHandler]): String = {
    val className = sockJsHandlerClass.getName
    if (className.endsWith("$")) {
      val normalClassName = className.substring(0, className.length - 1)
      val normalClass     = ClassUtils.getClass(normalClassName)
      sockJsPathPrefixForNormalSockJsHandlerClass(normalClass.asInstanceOf[Class[_ <: SockJsHandler]])
    } else {
      sockJsPathPrefixForNormalSockJsHandlerClass(sockJsHandlerClass)
    }
  }

  private def sockJsPathPrefixForNormalSockJsHandlerClass(sockJsHandlerClass: Class[_ <: SockJsHandler]): String = {
    val kv = sockJsRoutes.find { case (k, v) => v == sockJsHandlerClass }
    kv match {
      case Some((k, v)) => k
      case None         => throw new Exception("Cannot lookup SockJS URL for class: " + sockJsHandlerClass)
    }
  }
}
