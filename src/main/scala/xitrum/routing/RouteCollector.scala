package xitrum.routing

import scala.collection.mutable.{ArrayBuffer, Map => MMap}
import scala.util.control.NonFatal

import org.objectweb.asm.{AnnotationVisitor, ClassReader, ClassVisitor, Opcodes, Type}
import sclasner.{FileEntry, Scanner}

import xitrum.{Logger, SockJsActor}
import xitrum.annotation._
import xitrum.sockjs.SockJsPrefix

case class DiscoveredAcc(
  normalRoutes:              SerializableRouteCollection,
  sockJsWithoutPrefixRoutes: SerializableRouteCollection,
  sockJsMap:                 Map[String, SockJsClassAndOptions],
  parentClassCacheMap:       Map[String, Int]
)

/** Scan all classes to collect routes from actions. */
class RouteCollector extends Logger {
  def deserializeCacheFileOrRecollect(cachedFileName: String): DiscoveredAcc = {
    val acc = DiscoveredAcc(
        new SerializableRouteCollection,
        new SerializableRouteCollection,
        Map[String, SockJsClassAndOptions](),
        Map[String, Int]()
    )
    Scanner.foldLeft(cachedFileName, acc, discovered _)
  }

  //----------------------------------------------------------------------------

  private def discovered(acc: DiscoveredAcc, entry: FileEntry): DiscoveredAcc = {
    try {
      if (entry.relPath.endsWith(".class")) {
        val reader = new ClassReader(entry.bytes)
        processDiscovered(acc, reader)
      } else {
        acc
      }
    } catch {
      case NonFatal(e) =>
        logger.warn("Could not scan route for " + entry.relPath + " in " + entry.container, e)
        acc
    }
  }

  private def processDiscovered(acc: DiscoveredAcc, reader: ClassReader): DiscoveredAcc = {
    val className  = reader.getClassName.replace('/', '.')
    val fromSockJs = className.startsWith(classOf[SockJsPrefix].getPackage.getName)
    val routes     = if (fromSockJs) acc.sockJsWithoutPrefixRoutes else acc.normalRoutes

    // http://asm.ow2.org/asm40/javadoc/user/org/objectweb/asm/AnnotationVisitor.html
    //                     internalName  ->  annotation value (single value, not array)
    val annotations = MMap[String,           Object]()

    reader.accept(new ClassVisitor(Opcodes.ASM4) {
      override def visitAnnotation(desc: String, visible: Boolean): AnnotationVisitor = {
        if (desc.indexOf("xitrum/annotation") > -1) {
          annotations(desc) = null
          new AnnotationVisitor(Opcodes.ASM4) {
            override def visit(name: String, value: Object) {
              annotations(desc) = value
            }
          }
        } else {
          null
        }
      }
    }, 0)

    if (annotations.isEmpty) {
      acc
    } else {
      val newParentClassCacheMap = collectNormalRoutes(routes, acc.parentClassCacheMap, className, annotations)
      val newSockJsMap           = collectSockJsMap(acc.sockJsMap, className, annotations)
      collectErrorRoutes (routes, className, annotations)

      DiscoveredAcc(acc.normalRoutes, acc.sockJsWithoutPrefixRoutes, newSockJsMap, newParentClassCacheMap)
    }
  }

  private def collectNormalRoutes(
      routes:              SerializableRouteCollection,
      parentClassCacheMap: Map[String, Int],
      className:           String,
      annotations:         MMap[String, Object]
  ): Map[String, Int] =
  {
    var routeOrder          = 0  // -1: first, 1: last, 0: other
    var cacheSecs           = 0  // < 0: cache action, > 0: cache page, 0: no cache
    var method_pattern_coll = ArrayBuffer[(String, String)]()

    annotations.foreach { case (desc, value) =>
      optRouteOrder(desc)             .foreach { order => routeOrder = order }
      optCacheSecs(desc, value)       .foreach { secs  => cacheSecs  = secs  }
      optMethodAndPattern(desc, value).foreach { m_p   => method_pattern_coll.append(m_p) }
    }

    // Save cacheSecs, it may be used in direct subclass
    // https://github.com/ngocdaothanh/xitrum/issues/118
    if (method_pattern_coll.isEmpty && cacheSecs != 0) {
      val newParentClassCacheMap = parentClassCacheMap.updated(className, cacheSecs)
      return newParentClassCacheMap
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

    parentClassCacheMap
  }

  private def collectErrorRoutes(
      routes:      SerializableRouteCollection,
      className:   String,
      annotations: MMap[String, Object])
  {
    annotations.foreach { case (desc, _) =>
      if (desc == Type.getDescriptor(classOf[Error404])) routes.error404 = Some(className)
      if (desc == Type.getDescriptor(classOf[Error500])) routes.error500 = Some(className)
    }
  }

  private def collectSockJsMap(
      sockJsMap:   Map[String, SockJsClassAndOptions],
      className:   String,
      annotations: MMap[String, Object]
  ): Map[String, SockJsClassAndOptions] =
  {
    var pathPrefix: String = null
    var noWebSocket  = false
    var cookieNeeded = false

    annotations.foreach { case (desc, value) =>
      if (desc == Type.getDescriptor(classOf[SOCKJS]))             pathPrefix   = getString(value)
      if (desc == Type.getDescriptor(classOf[SockJsNoWebSocket]))  noWebSocket  = true
      if (desc == Type.getDescriptor(classOf[SockJsCookieNeeded])) cookieNeeded = true
    }

    if (pathPrefix == null) {
      sockJsMap
    } else {
      val klass = Class.forName(className).asInstanceOf[Class[SockJsActor]]
      sockJsMap + (pathPrefix -> new SockJsClassAndOptions(klass, !noWebSocket, cookieNeeded))
    }
  }

  //----------------------------------------------------------------------------

  private def optRouteOrder(annotationDesc: String): Option[Int] = {
    if (annotationDesc == Type.getDescriptor(classOf[First])) return Some(-1)
    if (annotationDesc == Type.getDescriptor(classOf[Last]))  return Some(1)
    None
  }

  private def optCacheSecs(annotationDesc: String, annotationValue: Object): Option[Int] = {
    if (annotationDesc == Type.getDescriptor(classOf[CacheActionDay]))
      return Some(-getInt(annotationValue) * 24 * 60 * 60)

    if (annotationDesc == Type.getDescriptor(classOf[CacheActionHour]))
      return Some(-getInt(annotationValue)      * 60 * 60)

    if (annotationDesc == Type.getDescriptor(classOf[CacheActionMinute]))
      return Some(-getInt(annotationValue)           * 60)

    if (annotationDesc == Type.getDescriptor(classOf[CacheActionSecond]))
      return Some(-getInt(annotationValue))

    if (annotationDesc == Type.getDescriptor(classOf[CachePageDay]))
      return Some(getInt(annotationValue) * 24 * 60 * 60)

    if (annotationDesc == Type.getDescriptor(classOf[CachePageHour]))
      return Some(getInt(annotationValue)      * 60 * 60)

    if (annotationDesc == Type.getDescriptor(classOf[CachePageMinute]))
      return Some(getInt(annotationValue)           * 60)

    if (annotationDesc == Type.getDescriptor(classOf[CachePageSecond]))
      return Some(getInt(annotationValue))

    None
  }

  /** @return Option[(method, pattern)] */
  private def optMethodAndPattern(annotationDesc: String, annotationValue: Object): Option[(String, String)] = {
    if (annotationDesc == Type.getDescriptor(classOf[GET]))
      return Some("GET", getString(annotationValue))

    if (annotationDesc == Type.getDescriptor(classOf[POST]))
      return Some("POST", getString(annotationValue))

    if (annotationDesc == Type.getDescriptor(classOf[PUT]))
      return Some("PUT", getString(annotationValue))

    if (annotationDesc == Type.getDescriptor(classOf[DELETE]))
      return Some("DELETE", getString(annotationValue))

    if (annotationDesc == Type.getDescriptor(classOf[OPTIONS]))
      return Some("OPTIONS", getString(annotationValue))

    if (annotationDesc == Type.getDescriptor(classOf[WEBSOCKET]))
      return Some("WEBSOCKET", getString(annotationValue))

    None
  }

  private def getInt(annotationValue: Object): Int =
    annotationValue.toString.toInt

  private def getString(annotationValue: Object): String =
    annotationValue.toString
}
