package xitrum.routing

import java.io.{ByteArrayInputStream, DataInputStream}
import java.lang.reflect.Method
import java.util.{List => JList}

import scala.collection.mutable.ArrayBuffer
import scala.util.control.NonFatal

import javassist.{ClassClassPath, ClassPool}
import javassist.bytecode.{AnnotationsAttribute, ClassFile, MethodInfo, AccessFlag}
import javassist.bytecode.annotation.{Annotation, MemberValue, ArrayMemberValue, StringMemberValue, IntegerMemberValue}
import sclasner.{FileEntry, Scanner}

import xitrum.{Action, Logger}
import xitrum.annotation._
import xitrum.sockjs.SockJsAction

/** Scan all classes to collect routes from actions. */
class RouteCollector extends Logger {
  def deserializeCacheFileOrRecollect(cachedFileName: String): (SerializableRouteCollection, SerializableRouteCollection) = {
    val normal = new SerializableRouteCollection
    val sockJs = new SerializableRouteCollection
    Scanner.foldLeft(cachedFileName, (normal, sockJs), discovered _)
  }

  //----------------------------------------------------------------------------

  private def discovered(
      normal_sockJs: (SerializableRouteCollection, SerializableRouteCollection),
      entry:         FileEntry): (SerializableRouteCollection, SerializableRouteCollection) =
  {
    try {
      if (entry.relPath.endsWith(".class")) {
        val bais = new ByteArrayInputStream(entry.bytes)
        val dis  = new DataInputStream(bais)
        val cf   = new ClassFile(dis)
        dis.close()
        bais.close()
        processDiscovered(normal_sockJs, cf)
        normal_sockJs
      } else {
        normal_sockJs
      }
    } catch {
      case NonFatal(e) =>
        logger.warn("Could not scan route for " + entry.relPath + " in " + entry.container, e)
        normal_sockJs
    }
  }

  private def processDiscovered(
      normal_sockJs: (SerializableRouteCollection, SerializableRouteCollection),
      classFile:     ClassFile)
  {
    val aa = classFile.getAttribute(AnnotationsAttribute.visibleTag).asInstanceOf[AnnotationsAttribute]
    if (aa == null) return

    val annotations = aa.getAnnotations
    val className   = classFile.getName
    val fromSockJs  = className.startsWith(classOf[SockJsAction].getPackage.getName)
    val routes      = if (fromSockJs) normal_sockJs._2 else normal_sockJs._1
    collectRoutes(routes, className, annotations)
  }

  private def collectRoutes(
      routes:      SerializableRouteCollection,
      className:   String,
      annotations: Array[Annotation])
  {
    var routeOrder           = 0  // -1: first, 1: last, 0: other
    var cacheSecs            = 0  // < 0: cache action, > 0: cache page, 0: no cache
    var method_pattern_coll = ArrayBuffer[(String, String)]()

    annotations.foreach { a =>
      val tn = a.getTypeName
      optRouteOrder(tn)         .foreach { order => routeOrder = order }
      optCacheSecs(a, tn)       .foreach { secs  => cacheSecs  = secs  }
      optMethodAndPattern(a, tn).foreach { m_ps  => method_pattern_coll.append(m_ps) }
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
  }

  private def optRouteOrder(annotationTypeName: String): Option[Int] = {
    if (annotationTypeName == classOf[First].getName) return Some(-1)
    if (annotationTypeName == classOf[Last].getName)  return Some(1)
    None
  }

  private def optCacheSecs(annotation: Annotation, annotationTypeName: String): Option[Int] = {
    if (annotationTypeName == classOf[CacheActionDay].getName)
      return Some(-getCacheSecsValue(annotation) * 24 * 60 * 60)

    if (annotationTypeName == classOf[CacheActionHour].getName)
      return Some(-getCacheSecsValue(annotation)      * 60 * 60)

    if (annotationTypeName == classOf[CacheActionMinute].getName)
      return Some(-getCacheSecsValue(annotation)           * 60)

    if (annotationTypeName == classOf[CacheActionSecond].getName)
      return Some(-getCacheSecsValue(annotation))

    if (annotationTypeName == classOf[CachePageDay].getName)
      return Some(getCacheSecsValue(annotation) * 24 * 60 * 60)

    if (annotationTypeName == classOf[CachePageHour].getName)
      return Some(getCacheSecsValue(annotation)      * 60 * 60)

    if (annotationTypeName == classOf[CachePageMinute].getName)
      return Some(getCacheSecsValue(annotation)           * 60)

    if (annotationTypeName == classOf[CachePageSecond].getName)
      return Some(getCacheSecsValue(annotation))

    None
  }

  /** @return Option[(method, pattern)] */
  private def optMethodAndPattern(annotation: Annotation, annotationTypeName: String): Option[(String, String)] = {
    if (annotationTypeName == classOf[GET].getName)
      return Some("GET", getMethodPattern(annotation))

    if (annotationTypeName == classOf[POST].getName)
      return Some("POST", getMethodPattern(annotation))

    if (annotationTypeName == classOf[PUT].getName)
      return Some("PUT", getMethodPattern(annotation))

    if (annotationTypeName == classOf[DELETE].getName)
      return Some("DELETE", getMethodPattern(annotation))

    if (annotationTypeName == classOf[OPTIONS].getName)
      return Some("OPTIONS", getMethodPattern(annotation))

    if (annotationTypeName == classOf[WEBSOCKET].getName)
      return Some("WEBSOCKET", getMethodPattern(annotation))

    None
  }

  private def getCacheSecsValue(annotation: Annotation): Int =
    annotation.getMemberValue("value").asInstanceOf[IntegerMemberValue].getValue

  private def getMethodPattern(annotation: Annotation): String =
    annotation.getMemberValue("value").asInstanceOf[StringMemberValue].getValue
}
