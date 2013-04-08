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
  def deserializeCacheFileOrRecollect(cachedFileName: String): RouteCollection = {
    val acc          = new SerializableRouteCollection
    val serializable = Scanner.foldLeft(cachedFileName, acc, discovered _)
    serializable.toRouteCollection
  }

/*
  def fromSockJsController: RouteCollection = {
    val pool = ClassPool.getDefault
    pool.insertClassPath(new ClassClassPath(classOf[SockJsController]))
    val cc = pool.get(classOf[SockJsController].getName)
    discover(true, Map[String, Seq[String]](), cc.getClassFile)
  }
*/

  //----------------------------------------------------------------------------

  private def discovered(acc: SerializableRouteCollection, entry: FileEntry): SerializableRouteCollection = {
    try {
      if (entry.relPath.endsWith(".class")) {
        val bais = new ByteArrayInputStream(entry.bytes)
        val dis  = new DataInputStream(bais)
        val cf   = new ClassFile(dis)
        dis.close()
        bais.close()
        processDiscovered(false, acc, cf)
        acc
      } else {
        acc
      }
    } catch {
      case NonFatal(e) =>
        logger.warn("Could not scan route for " + entry.relPath + " in " + entry.container, e)
        acc
    }
  }

  private def processDiscovered(fromSockJs: Boolean, acc: SerializableRouteCollection, classFile: ClassFile) {
    val className = classFile.getName
    if (!fromSockJs && className.startsWith(classOf[SockJsAction].getName)) return

    val aa = classFile.getAttribute(AnnotationsAttribute.visibleTag).asInstanceOf[AnnotationsAttribute]
    if (aa == null) return

    val as = aa.getAnnotations
    collectRoutes(acc, className, as)
  }

  private def collectRoutes(acc: SerializableRouteCollection, className: String, annotations: Array[Annotation]) {
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
        case (-1, "GET") => acc.firstGETs
        case ( 1, "GET") => acc.lastGETs
        case ( 0, "GET") => acc.otherGETs

        case (-1, "POST") => acc.firstPOSTs
        case ( 1, "POST") => acc.lastPOSTs
        case ( 0, "POST") => acc.otherPOSTs

        case (-1, "PUT") => acc.firstPUTs
        case ( 1, "PUT") => acc.lastPUTs
        case ( 0, "PUT") => acc.otherPUTs

        case (-1, "DELETE") => acc.firstDELETEs
        case ( 1, "DELETE") => acc.lastDELETEs
        case ( 0, "DELETE") => acc.otherDELETEs

        case (-1, "OPTIONS") => acc.firstOPTIONSs
        case ( 1, "OPTIONS") => acc.lastOPTIONSs
        case ( 0, "OPTIONS") => acc.otherOPTIONSs

        case (-1, "WEBSOCKET") => acc.firstWEBSOCKETs
        case ( 1, "WEBSOCKET") => acc.lastWEBSOCKETs
        case ( 0, "WEBSOCKET") => acc.otherWEBSOCKETs
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
