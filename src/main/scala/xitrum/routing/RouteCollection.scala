package xitrum.routing

import scala.annotation.tailrec
import scala.collection.mutable.{ArrayBuffer, Map => MMap}

import org.jboss.netty.handler.codec.http.HttpMethod

import xitrum.{Action, Logger}
import xitrum.scope.request.Params
import xitrum.scope.request.PathInfo

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

  val firstDELETEs: Seq[Route],
  val lastDELETEs:  Seq[Route],
  val otherDELETEs: Seq[Route],

  val firstOPTIONSs: Seq[Route],
  val lastOPTIONSs:  Seq[Route],
  val otherOPTIONSs: Seq[Route],

  val firstWEBSOCKETs: Seq[Route],
  val lastWEBSOCKETs:  Seq[Route],
  val otherWEBSOCKETs: Seq[Route],

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
    routeArray.appendAll(firstOPTIONSs)
    routeArray.appendAll(firstWEBSOCKETs)

    routeArray.appendAll(otherGETs)
    routeArray.appendAll(otherPOSTs)
    routeArray.appendAll(otherPUTs)
    routeArray.appendAll(otherOPTIONSs)
    routeArray.appendAll(otherWEBSOCKETs)

    routeArray.appendAll(lastGETs)
    routeArray.appendAll(lastPOSTs)
    routeArray.appendAll(lastPUTs)
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

    val firsts = ArrayBuffer[(String, String, String)]()
    var others = ArrayBuffer[(String, String, String)]()
    val lasts  = ArrayBuffer[(String, String, String)]()

    for (r <- allFirsts) firsts.append((r.httpMethod.toString, RouteCompiler.decompile(r.compiledPattern), r.klass.getName))
    for (r <- allOthers) others.append((r.httpMethod.toString, RouteCompiler.decompile(r.compiledPattern), r.klass.getName))
    for (r <- allLasts ) lasts .append((r.httpMethod.toString, RouteCompiler.decompile(r.compiledPattern), r.klass.getName))

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

  def printActionPageCaches() {
    // This method is only run once on start, speed is not a problem

    var actionCaches = ArrayBuffer[(String, Int)]()
    var actionMaxControllerActionNameLength = 0

    var pageCaches = ArrayBuffer[(String, Int)]()
    var pageMaxControllerActionNameLength = 0

    val all = allFirsts ++ allOthers ++ allLasts
    for (r <- all) {
      if (r.cacheSecs < 0) {
        val n = r.klass.toString
        actionCaches.append((n, -r.cacheSecs))

        val nLength = n.length
        if (nLength > actionMaxControllerActionNameLength) actionMaxControllerActionNameLength = nLength
      } else if (r.cacheSecs > 0) {
        val n = r.klass.toString
        pageCaches.append((n, r.cacheSecs))

        val nLength = n.length
        if (nLength > pageMaxControllerActionNameLength) pageMaxControllerActionNameLength = nLength
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

  def route(httpMethod: HttpMethod, pathInfo: PathInfo): Option[(Route, Params)] = {
    // This method is run for every request, thus should be fast

    matchMethod(httpMethod) match {
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
  }

  /** @return Option[(firsts, lasts, others)] */
  private def matchMethod(httpMethod: HttpMethod): Option[(Seq[Route], Seq[Route], Seq[Route])] = {
    val methodName = httpMethod.getName
    if (methodName == "GET")        return Some(firstGETs,       lastGETs,       otherGETs)
    if (methodName == "POST")       return Some(firstPOSTs,      lastPOSTs,      otherPOSTs)
    if (methodName == "PUT")        return Some(firstPUTs,       lastPUTs,       otherPUTs)
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
