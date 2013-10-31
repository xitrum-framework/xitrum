package xitrum.routing

import scala.annotation.tailrec
import scala.collection.mutable.{ArrayBuffer, Map => MMap}

import org.jboss.netty.handler.codec.http.HttpMethod

import xitrum.{Action, Log}
import xitrum.annotation.Swagger
import xitrum.scope.request.{Params, PathInfo}
import xitrum.util.LocalLruCache

object RouteCollection {
  def fromSerializable(acc: DiscoveredAcc): RouteCollection = {
    val normal              = acc.normalRoutes
    val sockJsWithoutPrefix = acc.sockJsWithoutPrefixRoutes
    val sockJsMap           = acc.sockJsMap
    val swaggerMap          = acc.swaggerMap

    // Add prefixes to SockJS routes
    sockJsMap.keys.foreach { prefix =>
      sockJsWithoutPrefix.firstGETs      .foreach { r => normal.firstGETs      .append(r.addPrefix(prefix)) }
      sockJsWithoutPrefix.firstPOSTs     .foreach { r => normal.firstPOSTs     .append(r.addPrefix(prefix)) }
      sockJsWithoutPrefix.firstPUTs      .foreach { r => normal.firstPUTs      .append(r.addPrefix(prefix)) }
      sockJsWithoutPrefix.firstPATCHs    .foreach { r => normal.firstPATCHs    .append(r.addPrefix(prefix)) }
      sockJsWithoutPrefix.firstDELETEs   .foreach { r => normal.firstDELETEs   .append(r.addPrefix(prefix)) }
      sockJsWithoutPrefix.firstWEBSOCKETs.foreach { r => normal.firstWEBSOCKETs.append(r.addPrefix(prefix)) }

      sockJsWithoutPrefix.lastGETs      .foreach { r => normal.lastGETs      .append(r.addPrefix(prefix)) }
      sockJsWithoutPrefix.lastPOSTs     .foreach { r => normal.lastPOSTs     .append(r.addPrefix(prefix)) }
      sockJsWithoutPrefix.lastPUTs      .foreach { r => normal.lastPUTs      .append(r.addPrefix(prefix)) }
      sockJsWithoutPrefix.lastPATCHs    .foreach { r => normal.lastPATCHs    .append(r.addPrefix(prefix)) }
      sockJsWithoutPrefix.lastDELETEs   .foreach { r => normal.lastDELETEs   .append(r.addPrefix(prefix)) }
      sockJsWithoutPrefix.lastWEBSOCKETs.foreach { r => normal.lastWEBSOCKETs.append(r.addPrefix(prefix)) }

      sockJsWithoutPrefix.otherGETs      .foreach { r => normal.otherGETs      .append(r.addPrefix(prefix)) }
      sockJsWithoutPrefix.otherPOSTs     .foreach { r => normal.otherPOSTs     .append(r.addPrefix(prefix)) }
      sockJsWithoutPrefix.otherPUTs      .foreach { r => normal.otherPUTs      .append(r.addPrefix(prefix)) }
      sockJsWithoutPrefix.otherPATCHs    .foreach { r => normal.otherPATCHs    .append(r.addPrefix(prefix)) }
      sockJsWithoutPrefix.otherDELETEs   .foreach { r => normal.otherDELETEs   .append(r.addPrefix(prefix)) }
      sockJsWithoutPrefix.otherWEBSOCKETs.foreach { r => normal.otherWEBSOCKETs.append(r.addPrefix(prefix)) }
    }

    new RouteCollection(
      normal.firstGETs      .map(_.toRoute), normal.lastGETs      .map(_.toRoute), normal.otherGETs      .map(_.toRoute),
      normal.firstPOSTs     .map(_.toRoute), normal.lastPOSTs     .map(_.toRoute), normal.otherPOSTs     .map(_.toRoute),
      normal.firstPUTs      .map(_.toRoute), normal.lastPUTs      .map(_.toRoute), normal.otherPUTs      .map(_.toRoute),
      normal.firstPATCHs    .map(_.toRoute), normal.lastPATCHs    .map(_.toRoute), normal.otherPATCHs    .map(_.toRoute),
      normal.firstDELETEs   .map(_.toRoute), normal.lastDELETEs   .map(_.toRoute), normal.otherDELETEs   .map(_.toRoute),
      normal.firstWEBSOCKETs.map(_.toRoute), normal.lastWEBSOCKETs.map(_.toRoute), normal.otherWEBSOCKETs.map(_.toRoute),
      new SockJsRouteMap(sockJsMap),
      swaggerMap,
      normal.error404.map(Class.forName(_).asInstanceOf[Class[Action]]),
      normal.error500.map(Class.forName(_).asInstanceOf[Class[Action]])
    )
  }
}

/** Direct listing is used, map is not used, so that route matching is faster. */
class RouteCollection(
  val firstGETs: Seq[Route],
  val lastGETs:  Seq[Route],
  val otherGETs: Seq[Route],

  val firstPOSTs: Seq[Route],
  val lastPOSTs:  Seq[Route],
  val otherPOSTs: Seq[Route],

  val firstPUTs: Seq[Route],
  val lastPUTs:  Seq[Route],
  val otherPUTs: Seq[Route],

  val firstPATCHs: Seq[Route],
  val lastPATCHs:  Seq[Route],
  val otherPATCHs: Seq[Route],

  val firstDELETEs: Seq[Route],
  val lastDELETEs:  Seq[Route],
  val otherDELETEs: Seq[Route],

  val firstWEBSOCKETs: Seq[Route],
  val lastWEBSOCKETs:  Seq[Route],
  val otherWEBSOCKETs: Seq[Route],

  val sockJsRouteMap: SockJsRouteMap,
  val swaggerMap:     Map[Class[_ <: Action], Swagger],

  // 404.html and 500.html are used by default
  val error404: Option[Class[Action]],
  val error500: Option[Class[Action]]
) extends Log
{
  lazy val reverseMappings: Map[Class[_], ReverseRoute] = {
    val mmap = MMap[Class[_], ArrayBuffer[Route]]()

    allFirsts(None).foreach { r => mmap.getOrElseUpdate(r.klass, ArrayBuffer()).append(r) }
    allOthers(None).foreach { r => mmap.getOrElseUpdate(r.klass, ArrayBuffer()).append(r) }
    allLasts (None).foreach { r => mmap.getOrElseUpdate(r.klass, ArrayBuffer()).append(r) }

    mmap.mapValues { routes => ReverseRoute(routes) }.toMap
  }

  // No need to store OPTIONS here
  // Value example: "GET, HEAD, POST"
  lazy val corsAllowMethods: Map[Class[_], String] = {
    val ret = MMap[Class[_], String]()

    val routeArray = ArrayBuffer[Route]()
    routeArray.appendAll(allFirsts(None))
    routeArray.appendAll(allOthers(None))
    routeArray.appendAll(allLasts(None))

    routeArray.foreach { r =>
      val klass  = r.klass
      val method = if (r.httpMethod.getName == "GET") "GET, HEAD" else r.httpMethod.getName

      ret.get(klass) match {
        case None =>
          ret(klass) = method

        case Some(methods) =>
          if (!methods.contains(method)) ret(klass) = methods + ", " + method
      }
    }

    ret.toMap
  }

  //----------------------------------------------------------------------------
  // Run only at startup, speed is not a problem

  /** @param xitrumRoutes true: log only Xitrum routes, false: log only app routes */
  def logRoutes(xitrumRoutes: Boolean) {
    // This method is only run once on start, speed is not a problem

    //                        method  pattern target
    val firsts = ArrayBuffer[(String, String, String)]()
    var others = ArrayBuffer[(String, String, String)]()
    val lasts  = ArrayBuffer[(String, String, String)]()

    for (r <- allFirsts(Some(xitrumRoutes))) firsts.append((r.httpMethod.toString, RouteCompiler.decompile(r.compiledPattern), targetWithCache(r)))
    for (r <- allOthers(Some(xitrumRoutes))) others.append((r.httpMethod.toString, RouteCompiler.decompile(r.compiledPattern), targetWithCache(r)))
    for (r <- allLasts (Some(xitrumRoutes))) lasts .append((r.httpMethod.toString, RouteCompiler.decompile(r.compiledPattern), targetWithCache(r)))

    // Sort by pattern
    var all = firsts ++ others.sortBy(_._2) ++ lasts

    val (methodHttpMaxLength, patternMaxLength) = all.foldLeft((0, 0)) { case ((mmax, pmax), (m, p, _)) =>
      val mlen  = m.length
      val plen  = p.length
      val mmax2 = if (mmax < mlen) mlen else mmax
      val pmax2 = if (pmax < plen) plen else pmax
      (mmax2, pmax2)
    }
    val logFormat = "%-" + methodHttpMaxLength + "s  %-" + patternMaxLength + "s  %s"

    val strings = all.map { case (m, p, cr) => logFormat.format(m, p, cr) }
    if (xitrumRoutes)
      log.info("Xitrum routes:\n" + strings.mkString("\n"))
    else
      log.info("Normal routes:\n" + strings.mkString("\n"))
  }

  private def targetWithCache(route: Route): String = {
    val target = route.klass.getName
    val secs   = route.cacheSecs
    if (secs == 0)
      target
    else if (secs < 0)
      s"$target (action cache: ${formatTime(-secs)})"
    else
      s"$target (page cache: ${formatTime(secs)})"
  }

  def logErrorRoutes() {
    val strings = ArrayBuffer[String]()
    error404.foreach { klass => strings.append("404  " + klass.getName) }
    error500.foreach { klass => strings.append("500  " + klass.getName) }
    if (!strings.isEmpty) log.info("Error routes:\n" + strings.mkString("\n"))
  }

  /**
   * @param xitrumRoutes
   * - None: No filter, return all routes
   * - Some(true): Only return Xitrum internal routes
   * - Some(false): Only return non Xitrum internal routes
   */
  private def allFirsts(xitrumRoutes: Option[Boolean]): Seq[Route] = {
    xitrumRoutes match {
      case None =>
        val ret = ArrayBuffer[Route]()
        ret.appendAll(firstGETs)
        ret.appendAll(firstPOSTs)
        ret.appendAll(firstPUTs)
        ret.appendAll(firstDELETEs)
        ret.appendAll(firstWEBSOCKETs)
        ret

      case Some(x) =>
        val ret = ArrayBuffer[Route]()
        ret.appendAll(firstGETs      .filter(_.klass.getName.startsWith("xitrum") == x))
        ret.appendAll(firstPOSTs     .filter(_.klass.getName.startsWith("xitrum") == x))
        ret.appendAll(firstPUTs      .filter(_.klass.getName.startsWith("xitrum") == x))
        ret.appendAll(firstDELETEs   .filter(_.klass.getName.startsWith("xitrum") == x))
        ret.appendAll(firstWEBSOCKETs.filter(_.klass.getName.startsWith("xitrum") == x))
        ret
    }
  }

  /** See allFirsts */
  private def allLasts(xitrumRoutes: Option[Boolean]): Seq[Route] = {
    xitrumRoutes match {
      case None =>
        val ret = ArrayBuffer[Route]()
        ret.appendAll(lastGETs)
        ret.appendAll(lastPOSTs)
        ret.appendAll(lastPUTs)
        ret.appendAll(lastDELETEs)
        ret.appendAll(lastWEBSOCKETs)
        ret

      case Some(x) =>
        val ret = ArrayBuffer[Route]()
        ret.appendAll(lastGETs      .filter(_.klass.getName.startsWith("xitrum") == x))
        ret.appendAll(lastPOSTs     .filter(_.klass.getName.startsWith("xitrum") == x))
        ret.appendAll(lastPUTs      .filter(_.klass.getName.startsWith("xitrum") == x))
        ret.appendAll(lastDELETEs   .filter(_.klass.getName.startsWith("xitrum") == x))
        ret.appendAll(lastWEBSOCKETs.filter(_.klass.getName.startsWith("xitrum") == x))
        ret
    }
  }

  /** See allFirsts */
  private def allOthers(xitrumRoutes: Option[Boolean]): Seq[Route] = {
    xitrumRoutes match {
      case None =>
        val ret = ArrayBuffer[Route]()
        ret.appendAll(otherGETs)
        ret.appendAll(otherPOSTs)
        ret.appendAll(otherPUTs)
        ret.appendAll(otherPATCHs)
        ret.appendAll(otherDELETEs)
        ret.appendAll(otherWEBSOCKETs)
        ret

      case Some(x) =>
        val ret = ArrayBuffer[Route]()
        ret.appendAll(otherGETs      .filter(_.klass.getName.startsWith("xitrum") == x))
        ret.appendAll(otherPOSTs     .filter(_.klass.getName.startsWith("xitrum") == x))
        ret.appendAll(otherPUTs      .filter(_.klass.getName.startsWith("xitrum") == x))
        ret.appendAll(otherPATCHs    .filter(_.klass.getName.startsWith("xitrum") == x))
        ret.appendAll(otherDELETEs   .filter(_.klass.getName.startsWith("xitrum") == x))
        ret.appendAll(otherWEBSOCKETs.filter(_.klass.getName.startsWith("xitrum") == x))
        ret
    }
  }

  private def formatTime(seconds: Int): String = {
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

  //----------------------------------------------------------------------------

  private val matchedRouteCache = LocalLruCache[String, (Route, Params)](1024)

  def route(httpMethod: HttpMethod, pathInfo: PathInfo): Option[(Route, Params)] = {
    // This method is run for every request, thus should be fast

    val key   = httpMethod + pathInfo.encoded
    val value = matchedRouteCache.get(key)
    if (value != null) return Some(value)

    val maybeCached = matchMethod(httpMethod) match {
      case None => None

      case Some((firsts, lasts, others)) =>
        val tokens = pathInfo.tokens

        matchAndExtractPathParams(tokens, firsts) match {
          case None =>
            matchAndExtractPathParams(tokens, others) match {
              case None => matchAndExtractPathParams(tokens, lasts)
              case s    => s
            }

          case s => s
        }
    }
    maybeCached.foreach { value => matchedRouteCache.put(key, value) }
    maybeCached
  }

  /** @return Option[(firsts, lasts, others)] */
  private def matchMethod(httpMethod: HttpMethod): Option[(Seq[Route], Seq[Route], Seq[Route])] = {
    val methodName = httpMethod.getName
    if (methodName == "GET")       return Some(firstGETs,       lastGETs,       otherGETs)
    if (methodName == "POST")      return Some(firstPOSTs,      lastPOSTs,      otherPOSTs)
    if (methodName == "PUT")       return Some(firstPUTs,       lastPUTs,       otherPUTs)
    if (methodName == "PATCH")     return Some(firstPATCHs,     lastPATCHs,     otherPATCHs)
    if (methodName == "DELETE")    return Some(firstDELETEs,    lastDELETEs,    otherDELETEs)
    if (methodName == "WEBSOCKET") return Some(firstWEBSOCKETs, lastWEBSOCKETs, otherWEBSOCKETs)
    None
  }

  /** @return Option[(Class[Action], cacheSecs, Params)] */
  @tailrec
  private def matchAndExtractPathParams(tokens: Array[String], routes: Seq[Route]): Option[(Route, Params)] = {
    if (routes.isEmpty) return None

    val route = routes.head
    route.matchRoute(tokens) match {
      case Some(params) => Some(route, params)
      case None         => matchAndExtractPathParams(tokens, routes.tail)
    }
  }
}
