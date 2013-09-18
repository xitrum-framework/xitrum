package xitrum.routing

import scala.collection.mutable.{ArrayBuffer, Map => MMap}
import scala.reflect.runtime.universe
import scala.util.control.NonFatal

import org.objectweb.asm.{AnnotationVisitor, ClassReader, ClassVisitor, Opcodes, Type}
import sclasner.{FileEntry, Scanner}

import xitrum.{Action, Logger, SockJsActor}
import xitrum.annotation._
import xitrum.sockjs.SockJsPrefix

case class DiscoveredAcc(
  normalRoutes:              SerializableRouteCollection,
  sockJsWithoutPrefixRoutes: SerializableRouteCollection,
  sockJsMap:                 Map[String, SockJsClassAndOptions]
)

/** Scan all classes to collect routes from actions. */
class RouteCollector extends Logger {
  def deserializeCacheFileOrRecollect(cachedFileName: String): DiscoveredAcc = {
    var acc = DiscoveredAcc(
      new SerializableRouteCollection,
      new SerializableRouteCollection,
      Map[String, SockJsClassAndOptions]()
    )

    val actionTreeBuilder = Scanner.foldLeft(cachedFileName, new ActionTreeBuilder, discovered _)
    actionTreeBuilder.traverse { case (klass, annotations) =>
      acc = processAnnotations(acc, klass, annotations)
    }

    acc
  }

  //----------------------------------------------------------------------------

  private def discovered(acc: ActionTreeBuilder, entry: FileEntry): ActionTreeBuilder = {
    try {
      if (entry.relPath.endsWith(".class")) {
        val reader = new ClassReader(entry.bytes)
        acc.addBranches(reader.getClassName, reader.getInterfaces)
      } else {
        acc
      }
    } catch {
      case NonFatal(e) =>
        logger.warn("Could not scan route for " + entry.relPath + " in " + entry.container, e)
        acc
    }
  }

  private def processAnnotations(
      acc:         DiscoveredAcc,
      klass:       Class[_ <: Action],
      annotations: Seq[universe.Annotation]
  ): DiscoveredAcc = {
    val className  = klass.getName
    val fromSockJs = className.startsWith(classOf[SockJsPrefix].getPackage.getName)
    val routes     = if (fromSockJs) acc.sockJsWithoutPrefixRoutes else acc.normalRoutes

    collectNormalRoutes(routes, className, annotations)
    val newSockJsMap = collectSockJsMap(acc.sockJsMap, className, annotations)
    collectErrorRoutes(routes, className, annotations)

    DiscoveredAcc(acc.normalRoutes, acc.sockJsWithoutPrefixRoutes, newSockJsMap)
  }

  private def collectNormalRoutes(
      routes:      SerializableRouteCollection,
      className:   String,
      annotations: Seq[universe.Annotation]
  ) {
    var routeOrder          = 0  // -1: first, 1: last, 0: other
    var cacheSecs           = 0  // < 0: cache action, > 0: cache page, 0: no cache
    var method_pattern_coll = ArrayBuffer[(String, String)]()

    annotations.foreach { a =>
      optRouteOrder(a)       .foreach { order => routeOrder = order }
      optCacheSecs(a)        .foreach { secs  => cacheSecs  = secs  }
      listMethodAndPattern(a).foreach { m_p   => method_pattern_coll.append(m_p) }
    }

    method_pattern_coll.foreach { case (method, pattern) =>
      val compiledPattern   = RouteCompiler.compile(pattern)
      val serializableRoute = new SerializableRoute(method, compiledPattern, className, cacheSecs)
      val coll              = (routeOrder, method) match {
        case (-1, "GET") => routes.firstGETs
        case ( 1, "GET") => routes.lastGETs
        case ( 0, "GET") => routes.otherGETs

        case (-1, "POST") => routes.firstPOSTs
        case ( 1, "POST") => routes.lastPOSTs
        case ( 0, "POST") => routes.otherPOSTs

        case (-1, "PUT") => routes.firstPUTs
        case ( 1, "PUT") => routes.lastPUTs
        case ( 0, "PUT") => routes.otherPUTs

        case (-1, "PATCH") => routes.firstPATCHs
        case ( 1, "PATCH") => routes.lastPATCHs
        case ( 0, "PATCH") => routes.otherPATCHs

        case (-1, "DELETE") => routes.firstDELETEs
        case ( 1, "DELETE") => routes.lastDELETEs
        case ( 0, "DELETE") => routes.otherDELETEs

        case (-1, "OPTIONS") => routes.firstOPTIONSs
        case ( 1, "OPTIONS") => routes.lastOPTIONSs
        case ( 0, "OPTIONS") => routes.otherOPTIONSs

        case (-1, "WEBSOCKET") => routes.firstWEBSOCKETs
        case ( 1, "WEBSOCKET") => routes.lastWEBSOCKETs
        case ( 0, "WEBSOCKET") => routes.otherWEBSOCKETs
      }
      coll.append(serializableRoute)
    }
  }

  private def collectErrorRoutes(
      routes:      SerializableRouteCollection,
      className:   String,
      annotations: Seq[universe.Annotation])
  {
    val tpeError404 = universe.typeOf[Error404]
    val tpeError500 = universe.typeOf[Error500]
    annotations.foreach { case a =>
      val tpe = a.tpe
      if (tpe == tpeError404) routes.error404 = Some(className)
      if (tpe == tpeError500) routes.error500 = Some(className)
    }
  }

  private def collectSockJsMap(
      sockJsMap:   Map[String, SockJsClassAndOptions],
      className:   String,
      annotations: Seq[universe.Annotation]
  ): Map[String, SockJsClassAndOptions] =
  {
    var pathPrefix: String = null
    var noWebSocket  = false
    var cookieNeeded = false

    val tpeSOCKJS             = universe.typeOf[SOCKJS]
    val tpeSockJsNoWebSocket  = universe.typeOf[SockJsNoWebSocket]
    val tpeSockJsCookieNeeded = universe.typeOf[SockJsCookieNeeded]
    annotations.foreach { case a =>
      val tpe = a.tpe
      if (tpe == tpeSOCKJS)             pathPrefix   = a.scalaArgs(0).productElement(0).asInstanceOf[universe.Constant].value.toString
      if (tpe == tpeSockJsNoWebSocket)  noWebSocket  = true
      if (tpe == tpeSockJsCookieNeeded) cookieNeeded = true
    }

    if (pathPrefix == null) {
      sockJsMap
    } else {
      val klass = Class.forName(className).asInstanceOf[Class[SockJsActor]]
      sockJsMap + (pathPrefix -> new SockJsClassAndOptions(klass, !noWebSocket, cookieNeeded))
    }
  }

  //----------------------------------------------------------------------------

  /** -1: first, 1: last, 0: other */
  private def optRouteOrder(annotation: universe.Annotation): Option[Int] = {
    val tpe = annotation.tpe
    if (tpe == universe.typeOf[First])
      Some(-1)
    else if (tpe == universe.typeOf[Last])
      Some(1)
    else
      None
  }

  /** < 0: cache action, > 0: cache page, 0: no cache */
  private def optCacheSecs(annotation: universe.Annotation): Option[Int] = {
    val tpe = annotation.tpe

    if (tpe == universe.typeOf[CacheActionDay])
      Some(-annotation.scalaArgs(0).toString.toInt * 24 * 60 * 60)
    else if (tpe == universe.typeOf[CacheActionHour])
      Some(-annotation.scalaArgs(0).toString.toInt      * 60 * 60)
    else if (tpe == universe.typeOf[CacheActionMinute])
      Some(-annotation.scalaArgs(0).toString.toInt           * 60)
    else if (tpe == universe.typeOf[CacheActionSecond])
      Some(-annotation.scalaArgs(0).toString.toInt)
    else if (tpe == universe.typeOf[CachePageDay])
      Some(annotation.scalaArgs(0).toString.toInt * 24 * 60 * 60)
    else if (tpe == universe.typeOf[CachePageHour])
      Some(annotation.scalaArgs(0).toString.toInt      * 60 * 60)
    else if (tpe == universe.typeOf[CachePageMinute])
      Some(annotation.scalaArgs(0).toString.toInt           * 60)
    else if (tpe == universe.typeOf[CachePageSecond])
      Some(annotation.scalaArgs(0).toString.toInt)
    else
      None
  }

  /** @return Seq[(method, pattern)] */
  private def listMethodAndPattern(annotation: universe.Annotation): Seq[(String, String)] = {
    val tpe = annotation.tpe

    if (tpe == universe.typeOf[GET])
      getStrings(annotation).map(("GET", _))
    else if (tpe == universe.typeOf[POST])
      getStrings(annotation).map(("POST", _))
    else if (tpe == universe.typeOf[PUT])
      getStrings(annotation).map(("PUT", _))
    else if (tpe == universe.typeOf[PATCH])
      getStrings(annotation).map(("PATCH", _))
    else if (tpe == universe.typeOf[DELETE])
      getStrings(annotation).map(("DELETE", _))
    else if (tpe == universe.typeOf[OPTIONS])
      getStrings(annotation).map(("OPTIONS", _))
    else if (tpe == universe.typeOf[WEBSOCKET])
      getStrings(annotation).map(("WEBSOCKET", _))
    else
      Seq()
  }

  private def getStrings(annotation: universe.Annotation): Seq[String] = {
    annotation.scalaArgs.map { tree => tree.productElement(0).asInstanceOf[universe.Constant].value.toString }
  }
}
