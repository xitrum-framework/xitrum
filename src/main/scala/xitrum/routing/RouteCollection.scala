package xitrum.routing

import scala.annotation.tailrec
import scala.collection.mutable.{ArrayBuffer, Map => MMap}

import io.netty.handler.codec.http.HttpMethod

import xitrum.{Action, Log}
import xitrum.annotation.Swagger
import xitrum.scope.request.{Params, PathInfo}
import xitrum.util.LocalLruCache

object RouteCollection {
  def fromSerializable(acc: DiscoveredAcc, withSwagger: Boolean): RouteCollection = {
    val normal              = acc.normalRoutes
    val sockJsWithoutPrefix = acc.sockJsWithoutPrefixRoutes
    val sockJsMap           = acc.sockJsMap

    val swaggerMap: Map[Class[_ <: Action], Swagger] = if (withSwagger) acc.swaggerMap else Map.empty

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

    val firstGETs =
      if (withSwagger)
        normal.firstGETs
      else
        normal.firstGETs.filterNot { r =>
          val className = r.actionClass
          className == classOf[SwaggerJson].getName || className == classOf[SwaggerUi].getName
        }

    val cl = Thread.currentThread.getContextClassLoader
    new RouteCollection(
      firstGETs             .map(_.toRoute), normal.lastGETs      .map(_.toRoute), normal.otherGETs      .map(_.toRoute),
      normal.firstPOSTs     .map(_.toRoute), normal.lastPOSTs     .map(_.toRoute), normal.otherPOSTs     .map(_.toRoute),
      normal.firstPUTs      .map(_.toRoute), normal.lastPUTs      .map(_.toRoute), normal.otherPUTs      .map(_.toRoute),
      normal.firstPATCHs    .map(_.toRoute), normal.lastPATCHs    .map(_.toRoute), normal.otherPATCHs    .map(_.toRoute),
      normal.firstDELETEs   .map(_.toRoute), normal.lastDELETEs   .map(_.toRoute), normal.otherDELETEs   .map(_.toRoute),
      normal.firstWEBSOCKETs.map(_.toRoute), normal.lastWEBSOCKETs.map(_.toRoute), normal.otherWEBSOCKETs.map(_.toRoute),
      new SockJsRouteMap(MMap(sockJsMap.toSeq: _*)),
      swaggerMap,
      normal.error404.map(cl.loadClass(_).asInstanceOf[Class[Action]]),
      normal.error500.map(cl.loadClass(_).asInstanceOf[Class[Action]])
    )
  }
}

/**
 * Routes are grouped by methods.
 * The routes are `ArrayBuffer` so that routes can be modified after collected.
 */
class RouteCollection(
  val firstGETs: ArrayBuffer[Route],
  val lastGETs:  ArrayBuffer[Route],
  val otherGETs: ArrayBuffer[Route],

  val firstPOSTs: ArrayBuffer[Route],
  val lastPOSTs:  ArrayBuffer[Route],
  val otherPOSTs: ArrayBuffer[Route],

  val firstPUTs: ArrayBuffer[Route],
  val lastPUTs:  ArrayBuffer[Route],
  val otherPUTs: ArrayBuffer[Route],

  val firstPATCHs: ArrayBuffer[Route],
  val lastPATCHs:  ArrayBuffer[Route],
  val otherPATCHs: ArrayBuffer[Route],

  val firstDELETEs: ArrayBuffer[Route],
  val lastDELETEs:  ArrayBuffer[Route],
  val otherDELETEs: ArrayBuffer[Route],

  val firstWEBSOCKETs: ArrayBuffer[Route],
  val lastWEBSOCKETs:  ArrayBuffer[Route],
  val otherWEBSOCKETs: ArrayBuffer[Route],

  val sockJsRouteMap: SockJsRouteMap,
  val swaggerMap:     Map[Class[_ <: Action], Swagger],

  // 404.html and 500.html are used by default
  val error404: Option[Class[Action]],
  val error500: Option[Class[Action]]
)
{
  /**
   * Class name -> ReverseRoute
   *
   * Use class name (String) instead of Class[_] becasuse we want to reload
   * classes in development mode, but classes loaded by different class loaders
   * can't be compared.
   */
  lazy val reverseMappings: scala.collection.Map[String, ReverseRoute] = {
    val mmap = MMap.empty[String, ArrayBuffer[Route]]

    allFirsts(None).foreach { r => mmap.getOrElseUpdate(r.klass.getName, ArrayBuffer()).append(r) }
    allOthers(None).foreach { r => mmap.getOrElseUpdate(r.klass.getName, ArrayBuffer()).append(r) }
    allLasts (None).foreach { r => mmap.getOrElseUpdate(r.klass.getName, ArrayBuffer()).append(r) }

    mmap.mapValues { routes => ReverseRoute(routes) }
  }

  //----------------------------------------------------------------------------

  /**
   * All routes in one place for ease of use. Elements are `ArrayBuffer` and can
   * still be modified.
   */
  val all: Seq[ArrayBuffer[Route]] = Seq(
    firstGETs, firstPOSTs, firstPUTs, firstPATCHs, firstDELETEs, firstWEBSOCKETs,
    otherGETs, otherPOSTs, otherPUTs, otherPATCHs, otherDELETEs, otherWEBSOCKETs,
    lastGETs, lastPOSTs, lastPUTs, lastPATCHs, lastDELETEs, lastWEBSOCKETs
  )

  def allFlatten(): Seq[Route] = all.flatten

  /**
   * @param xitrumRoutes
   * - None: No filter, return all routes
   * - Some(true): Only return Xitrum internal routes
   * - Some(false): Only return non Xitrum internal routes
   */
  def allFirsts(xitrumRoutes: Option[Boolean]): Seq[Route] = {
    xitrumRoutes match {
      case None =>
        val ret = ArrayBuffer.empty[Route]
        ret.appendAll(firstGETs)
        ret.appendAll(firstPOSTs)
        ret.appendAll(firstPUTs)
        ret.appendAll(firstDELETEs)
        ret.appendAll(firstWEBSOCKETs)
        ret

      case Some(x) =>
        val ret = ArrayBuffer.empty[Route]
        ret.appendAll(firstGETs      .filter(_.klass.getName.startsWith("xitrum") == x))
        ret.appendAll(firstPOSTs     .filter(_.klass.getName.startsWith("xitrum") == x))
        ret.appendAll(firstPUTs      .filter(_.klass.getName.startsWith("xitrum") == x))
        ret.appendAll(firstDELETEs   .filter(_.klass.getName.startsWith("xitrum") == x))
        ret.appendAll(firstWEBSOCKETs.filter(_.klass.getName.startsWith("xitrum") == x))
        ret
    }
  }

  /** See allFirsts */
  def allLasts(xitrumRoutes: Option[Boolean]): Seq[Route] = {
    xitrumRoutes match {
      case None =>
        val ret = ArrayBuffer.empty[Route]
        ret.appendAll(lastGETs)
        ret.appendAll(lastPOSTs)
        ret.appendAll(lastPUTs)
        ret.appendAll(lastDELETEs)
        ret.appendAll(lastWEBSOCKETs)
        ret

      case Some(x) =>
        val ret = ArrayBuffer.empty[Route]
        ret.appendAll(lastGETs      .filter(_.klass.getName.startsWith("xitrum") == x))
        ret.appendAll(lastPOSTs     .filter(_.klass.getName.startsWith("xitrum") == x))
        ret.appendAll(lastPUTs      .filter(_.klass.getName.startsWith("xitrum") == x))
        ret.appendAll(lastDELETEs   .filter(_.klass.getName.startsWith("xitrum") == x))
        ret.appendAll(lastWEBSOCKETs.filter(_.klass.getName.startsWith("xitrum") == x))
        ret
    }
  }

  /** See allFirsts */
  def allOthers(xitrumRoutes: Option[Boolean]): Seq[Route] = {
    xitrumRoutes match {
      case None =>
        val ret = ArrayBuffer.empty[Route]
        ret.appendAll(otherGETs)
        ret.appendAll(otherPOSTs)
        ret.appendAll(otherPUTs)
        ret.appendAll(otherPATCHs)
        ret.appendAll(otherDELETEs)
        ret.appendAll(otherWEBSOCKETs)
        ret

      case Some(x) =>
        val ret = ArrayBuffer.empty[Route]
        ret.appendAll(otherGETs      .filter(_.klass.getName.startsWith("xitrum") == x))
        ret.appendAll(otherPOSTs     .filter(_.klass.getName.startsWith("xitrum") == x))
        ret.appendAll(otherPUTs      .filter(_.klass.getName.startsWith("xitrum") == x))
        ret.appendAll(otherPATCHs    .filter(_.klass.getName.startsWith("xitrum") == x))
        ret.appendAll(otherDELETEs   .filter(_.klass.getName.startsWith("xitrum") == x))
        ret.appendAll(otherWEBSOCKETs.filter(_.klass.getName.startsWith("xitrum") == x))
        ret
    }
  }

  //----------------------------------------------------------------------------
  // Run only at startup, speed is not a problem

  def logAll() {
    logRoutes(false)
    sockJsRouteMap.logRoutes(false)
    logErrorRoutes()

    logRoutes(true)
    sockJsRouteMap.logRoutes(true)
  }

  /** @param xitrumRoutes true: log only Xitrum routes, false: log only app routes */
  def logRoutes(xitrumRoutes: Boolean) {
    // This method is only run once on start, speed is not a problem

    //                              method  pattern target
    val firsts = ArrayBuffer.empty[(String, String, String)]
    val others = ArrayBuffer.empty[(String, String, String)]
    val lasts  = ArrayBuffer.empty[(String, String, String)]

    val (rFirsts, rOthers,rLasts) = if (xitrumRoutes) {
      // Filter out routes created for SockJS to avoid noisy log
      // (they are logged separately by sockJsRouteMap.logRoutes)
      (
        allFirsts(Some(xitrumRoutes)).filter(!_.klass.getName.startsWith("xitrum.sockjs")),
        allOthers(Some(xitrumRoutes)).filter(!_.klass.getName.startsWith("xitrum.sockjs")),
        allLasts (Some(xitrumRoutes)).filter(!_.klass.getName.startsWith("xitrum.sockjs"))
      )
    } else {
      (
        allFirsts(Some(xitrumRoutes)),
        allOthers(Some(xitrumRoutes)),
        allLasts (Some(xitrumRoutes))
      )
    }

    for (r <- rFirsts) firsts.append((r.httpMethod.name, RouteCompiler.decompile(r.compiledPattern), targetWithCache(r)))
    for (r <- rOthers) others.append((r.httpMethod.name, RouteCompiler.decompile(r.compiledPattern), targetWithCache(r)))
    for (r <- rLasts)  lasts .append((r.httpMethod.name, RouteCompiler.decompile(r.compiledPattern), targetWithCache(r)))

    // Sort by pattern
    val all = firsts ++ others.sortBy(_._2) ++ lasts

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
      Log.info("Xitrum routes:\n" + strings.mkString("\n"))
    else
      Log.info("Normal routes:\n" + strings.mkString("\n"))
  }

  def logErrorRoutes() {
    val strings = ArrayBuffer.empty[String]
    error404.foreach { klass => strings.append("404  " + klass.getName) }
    error500.foreach { klass => strings.append("500  " + klass.getName) }
    if (strings.nonEmpty) Log.info("Error routes:\n" + strings.mkString("\n"))
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

  // Cache recently matched routes to speed up route matching
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
              case some => some
            }

          case some => some
        }
    }
    maybeCached.foreach { value => matchedRouteCache.put(key, value) }
    maybeCached
  }

  /** @return Option[(firsts, lasts, others)] */
  private def matchMethod(httpMethod: HttpMethod): Option[(Seq[Route], Seq[Route], Seq[Route])] = {
    val methodName = httpMethod.name
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

  //----------------------------------------------------------------------------

  /** Used at SetCORS & OPTIONSResponse. */
  def tryAllMethods(pathInfo: PathInfo): Seq[HttpMethod] = {
    var methods = Seq.empty[HttpMethod]

    if (route(HttpMethod.GET, pathInfo).nonEmpty)
      methods = methods :+ HttpMethod.GET :+ HttpMethod.HEAD

    if (route(HttpMethod.POST, pathInfo).nonEmpty)
      methods = methods :+ HttpMethod.POST

    if (route(HttpMethod.PUT, pathInfo).nonEmpty)
      methods = methods :+ HttpMethod.PUT

    if (route(HttpMethod.PATCH, pathInfo).nonEmpty)
      methods = methods :+ HttpMethod.PATCH

    if (route(HttpMethod.DELETE, pathInfo).nonEmpty)
      methods = methods :+ HttpMethod.DELETE

    methods
  }

  //----------------------------------------------------------------------------
  // Convenient methods for modifying routes.

  /** removeByClass[ActionClassToRemove]()  */
  def removeByClass[A <: Action]()(implicit action: Manifest[A]) {
    val className = action.toString
    all.foreach { routes =>
      val tobeRemoved = routes.filter(_.klass.getName == className)
      routes --= tobeRemoved
    }
  }

  /** removeByPrefix("/path/prefix") or removeByPrefix("path/prefix") */
  def removeByPrefix(prefix: String) {
    val withoutSlashPrefix = if (prefix.startsWith("/")) prefix.substring(1) else prefix

    all.foreach { routes =>
      val tobeRemoved = routes.filter { r =>
        val nonDotRouteTokens = r.compiledPattern.takeWhile { t =>
          if (!t.isInstanceOf[NonDotRouteToken]) {
            false
          } else {
            val nd = t.asInstanceOf[NonDotRouteToken]
            !nd.isPlaceholder
          }
        }

        if (nonDotRouteTokens.isEmpty) {
          false
        } else {
          val values = nonDotRouteTokens.map(_.asInstanceOf[NonDotRouteToken].value)
          values.mkString("/").startsWith(withoutSlashPrefix)
        }
      }

      routes --= tobeRemoved
    }
    sockJsRouteMap.removeByPrefix(withoutSlashPrefix)
  }
}
