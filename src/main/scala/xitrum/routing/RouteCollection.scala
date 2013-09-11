package xitrum.routing

import scala.annotation.tailrec
import scala.collection.mutable.{ArrayBuffer, Map => MMap}

import org.jboss.netty.handler.codec.http.HttpMethod

import xitrum.{Action, Logger}
import xitrum.scope.request.{Params, PathInfo}
import xitrum.util.LocalLRUCache

object RouteCollection {
  def fromSerializable(acc: DiscoveredAcc): RouteCollection = {
    val normal              = acc.normalRoutes
    val sockJsWithoutPrefix = acc.sockJsWithoutPrefixRoutes
    val sockJsMap           = acc.sockJsMap
    val parentClassCacheMap = acc.parentClassCacheMap

    // Add prefixes to SockJS routes
    sockJsMap.keys.foreach { prefix =>
      sockJsWithoutPrefix.firstGETs      .foreach { r => normal.firstGETs      .append(r.addPrefix(prefix)) }
      sockJsWithoutPrefix.firstPOSTs     .foreach { r => normal.firstPOSTs     .append(r.addPrefix(prefix)) }
      sockJsWithoutPrefix.firstPUTs      .foreach { r => normal.firstPUTs      .append(r.addPrefix(prefix)) }
      sockJsWithoutPrefix.firstPATCHs    .foreach { r => normal.firstPATCHs    .append(r.addPrefix(prefix)) }
      sockJsWithoutPrefix.firstDELETEs   .foreach { r => normal.firstDELETEs   .append(r.addPrefix(prefix)) }
      sockJsWithoutPrefix.firstOPTIONSs  .foreach { r => normal.firstOPTIONSs  .append(r.addPrefix(prefix)) }
      sockJsWithoutPrefix.firstWEBSOCKETs.foreach { r => normal.firstWEBSOCKETs.append(r.addPrefix(prefix)) }

      sockJsWithoutPrefix.lastGETs      .foreach { r => normal.lastGETs      .append(r.addPrefix(prefix)) }
      sockJsWithoutPrefix.lastPOSTs     .foreach { r => normal.lastPOSTs     .append(r.addPrefix(prefix)) }
      sockJsWithoutPrefix.lastPUTs      .foreach { r => normal.lastPUTs      .append(r.addPrefix(prefix)) }
      sockJsWithoutPrefix.lastPATCHs    .foreach { r => normal.lastPATCHs    .append(r.addPrefix(prefix)) }
      sockJsWithoutPrefix.lastDELETEs   .foreach { r => normal.lastDELETEs   .append(r.addPrefix(prefix)) }
      sockJsWithoutPrefix.lastOPTIONSs  .foreach { r => normal.lastOPTIONSs  .append(r.addPrefix(prefix)) }
      sockJsWithoutPrefix.lastWEBSOCKETs.foreach { r => normal.lastWEBSOCKETs.append(r.addPrefix(prefix)) }

      sockJsWithoutPrefix.otherGETs      .foreach { r => normal.otherGETs      .append(r.addPrefix(prefix)) }
      sockJsWithoutPrefix.otherPOSTs     .foreach { r => normal.otherPOSTs     .append(r.addPrefix(prefix)) }
      sockJsWithoutPrefix.otherPUTs      .foreach { r => normal.otherPUTs      .append(r.addPrefix(prefix)) }
      sockJsWithoutPrefix.otherPATCHs    .foreach { r => normal.otherPATCHs    .append(r.addPrefix(prefix)) }
      sockJsWithoutPrefix.otherDELETEs   .foreach { r => normal.otherDELETEs   .append(r.addPrefix(prefix)) }
      sockJsWithoutPrefix.otherOPTIONSs  .foreach { r => normal.otherOPTIONSs  .append(r.addPrefix(prefix)) }
      sockJsWithoutPrefix.otherWEBSOCKETs.foreach { r => normal.otherWEBSOCKETs.append(r.addPrefix(prefix)) }
    }

    // Update normal routes with cacheSecs from parentClassCacheMap
    Seq(
      normal.firstGETs, normal.firstPOSTs, normal.firstPUTs, normal.firstPATCHs, normal.firstDELETEs, normal.firstOPTIONSs,
      normal.lastGETs,  normal.lastPOSTs,  normal.lastPUTs,  normal.lastPATCHs,  normal.lastDELETEs,  normal.lastOPTIONSs,
      normal.otherGETs, normal.otherPOSTs, normal.otherPUTs, normal.otherPATCHs, normal.otherDELETEs, normal.otherOPTIONSs
    ).foreach { rs =>
      rs.foreach { r =>
        if (r.cacheSecs == 0) r.cacheSecs = getCacheSecsFromParent(r.actionClass, parentClassCacheMap)
      }
    }

    new RouteCollection(
      normal.firstGETs      .map(_.toRoute), normal.lastGETs      .map(_.toRoute), normal.otherGETs      .map(_.toRoute),
      normal.firstPOSTs     .map(_.toRoute), normal.lastPOSTs     .map(_.toRoute), normal.otherPOSTs     .map(_.toRoute),
      normal.firstPUTs      .map(_.toRoute), normal.lastPUTs      .map(_.toRoute), normal.otherPUTs      .map(_.toRoute),
      normal.firstPATCHs    .map(_.toRoute), normal.lastPATCHs    .map(_.toRoute), normal.otherPATCHs    .map(_.toRoute),
      normal.firstDELETEs   .map(_.toRoute), normal.lastDELETEs   .map(_.toRoute), normal.otherDELETEs   .map(_.toRoute),
      normal.firstOPTIONSs  .map(_.toRoute), normal.lastOPTIONSs  .map(_.toRoute), normal.otherOPTIONSs  .map(_.toRoute),
      normal.firstWEBSOCKETs.map(_.toRoute), normal.lastWEBSOCKETs.map(_.toRoute), normal.otherWEBSOCKETs.map(_.toRoute),
      new SockJsRouteMap(sockJsMap),
      normal.error404.map(Class.forName(_).asInstanceOf[Class[Action]]),
      normal.error500.map(Class.forName(_).asInstanceOf[Class[Action]])
    )
  }

  private def getCacheSecsFromParent(className: String, parentClassCacheMap: Map[String, Int]): Int = {
    val klass      = Class.forName(className)
    val superClass = klass.getSuperclass.getName
    if (parentClassCacheMap.contains(superClass)) return parentClassCacheMap(superClass)

    val found = klass.getInterfaces.map(_.getName).find { i => parentClassCacheMap.contains(i) }
    found match {
      case None    => 0
      case Some(i) => parentClassCacheMap(i)
    }
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

  val firstOPTIONSs: Seq[Route],
  val lastOPTIONSs:  Seq[Route],
  val otherOPTIONSs: Seq[Route],

  val firstWEBSOCKETs: Seq[Route],
  val lastWEBSOCKETs:  Seq[Route],
  val otherWEBSOCKETs: Seq[Route],

  val sockJsRouteMap: SockJsRouteMap,

  // 404.html and 500.html are used by default
  val error404: Option[Class[Action]],
  val error500: Option[Class[Action]]
) extends Logger
{
  lazy val reverseMappings: Map[Class[_], Route] = {
    val mmap = MMap[Class[_], Route]()
    allFirsts.foreach { r => mmap(r.klass) = r }
    allLasts .foreach { r => mmap(r.klass) = r }
    allOthers.foreach { r => mmap(r.klass) = r }
    mmap.toMap
  }

  /** For use from browser */
  lazy val jsRoutes = {
    val routeArray = ArrayBuffer[Route]()

    routeArray.appendAll(firstGETs)
    routeArray.appendAll(firstPOSTs)
    routeArray.appendAll(firstPUTs)
    routeArray.appendAll(firstPATCHs)
    routeArray.appendAll(firstOPTIONSs)
    routeArray.appendAll(firstWEBSOCKETs)

    routeArray.appendAll(otherGETs)
    routeArray.appendAll(otherPOSTs)
    routeArray.appendAll(otherPUTs)
    routeArray.appendAll(otherPATCHs)
    routeArray.appendAll(otherOPTIONSs)
    routeArray.appendAll(otherWEBSOCKETs)

    routeArray.appendAll(lastGETs)
    routeArray.appendAll(lastPOSTs)
    routeArray.appendAll(lastPUTs)
    routeArray.appendAll(lastPATCHs)
    routeArray.appendAll(lastOPTIONSs)
    routeArray.appendAll(lastWEBSOCKETs)

    val xs = routeArray.map { route =>
      val ys = route.compiledPattern.map { rt =>
        "['" + rt.value + "', " + rt.isPlaceHolder + "]"
      }
      "[[" + ys.mkString(", ") + "], '" + route.klass.getName + "']"
    }
    "[" + xs.mkString(", ") + "]"
  }

  //----------------------------------------------------------------------------

  def printRoutes() {
    // This method is only run once on start, speed is not a problem

    //                        method  pattern target
    val firsts = ArrayBuffer[(String, String, String)]()
    var others = ArrayBuffer[(String, String, String)]()
    val lasts  = ArrayBuffer[(String, String, String)]()

    for (r <- allFirsts) firsts.append((r.httpMethod.toString, RouteCompiler.decompile(r.compiledPattern), targetWithCache(r)))
    for (r <- allOthers) others.append((r.httpMethod.toString, RouteCompiler.decompile(r.compiledPattern), targetWithCache(r)))
    for (r <- allLasts ) lasts .append((r.httpMethod.toString, RouteCompiler.decompile(r.compiledPattern), targetWithCache(r)))

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
    logger.info("Routes:\n" + strings.mkString("\n"))
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

  def printErrorRoutes() {
    val strings = ArrayBuffer[String]()
    error404.foreach { klass => strings.append("404  " + klass.getName) }
    error500.foreach { klass => strings.append("500  " + klass.getName) }
    if (!strings.isEmpty) logger.info("Error routes:\n" + strings.mkString("\n"))
  }

  private def allFirsts(): Seq[Route] = {
    val ret = ArrayBuffer[Route]()
    ret.appendAll(firstGETs)
    ret.appendAll(firstPOSTs)
    ret.appendAll(firstPUTs)
    ret.appendAll(firstDELETEs)
    ret.appendAll(firstOPTIONSs)
    ret.appendAll(firstWEBSOCKETs)
    ret
  }

  private def allLasts(): Seq[Route] = {
    val ret = ArrayBuffer[Route]()
    ret.appendAll(lastGETs)
    ret.appendAll(lastPOSTs)
    ret.appendAll(lastPUTs)
    ret.appendAll(lastDELETEs)
    ret.appendAll(lastOPTIONSs)
    ret.appendAll(lastWEBSOCKETs)
    ret
  }

  private def allOthers(): Seq[Route] = {
    val ret = ArrayBuffer[Route]()
    ret.appendAll(otherGETs)
    ret.appendAll(otherPOSTs)
    ret.appendAll(otherPUTs)
    ret.appendAll(otherPATCHs)
    ret.appendAll(otherDELETEs)
    ret.appendAll(otherOPTIONSs)
    ret.appendAll(otherWEBSOCKETs)
    ret
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

  private val matchedRouteCache = LocalLRUCache[String, (Route, Params)](1000)

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
    if (methodName == "GET")        return Some(firstGETs,       lastGETs,       otherGETs)
    if (methodName == "POST")       return Some(firstPOSTs,      lastPOSTs,      otherPOSTs)
    if (methodName == "PUT")        return Some(firstPUTs,       lastPUTs,       otherPUTs)
    if (methodName == "PATCH")      return Some(firstPATCHs,     lastPATCHs,     otherPATCHs)
    if (methodName == "DELETE")     return Some(firstDELETEs,    lastDELETEs,    otherDELETEs)
    if (methodName == "OPTIONS")    return Some(firstOPTIONSs,   lastOPTIONSs,   otherOPTIONSs)
    if (methodName == "WEBSOCKET")  return Some(firstWEBSOCKETs, lastWEBSOCKETs, otherWEBSOCKETs)
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
