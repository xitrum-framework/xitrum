package xitrum.action.routing

import java.lang.annotation.{Annotation => JAnnotation}
import scala.collection.mutable.{ArrayBuffer, HashMap => MHashMap}

import org.jboss.netty.handler.codec.http.HttpMethod

import com.impetus.annovention.{Discoverer, ClasspathDiscoverer}
import com.impetus.annovention.listener.ClassAnnotationDiscoveryListener

import xitrum.action.Action
import xitrum.action.annotation._
import xitrum.action.cache._

/** Scan all classes to collect routes. */
class RouteCollector extends ClassAnnotationDiscoveryListener {
  type RouteMap = MHashMap[Class[Action], (HttpMethod, Array[Routes.Pattern], Option[Cache])]

  private val firsts = new RouteMap
  private val lasts  = new RouteMap
  private val others = new RouteMap

  def collect: Array[Routes.Route]  = {
    val discoverer = new ClasspathDiscoverer
    discoverer.addAnnotationListener(this)
    discoverer.discover

    val buffer = new ArrayBuffer[Routes.Route]

    // Make POST2Action the first route for quicker route matching
    buffer.append((HttpMethod.POST, Routes.POST2_PREFIX + ":*", classOf[POST2Action].asInstanceOf[Class[Action]], None))

    for (map <- Array(firsts, others, lasts)) {
      val sorted = map.toBuffer.sortWith { (a1, a2) =>
        a1.toString < a2.toString
      }

      for ((actionClass, httpMethod_patterns_cacheo) <- sorted) {
        val (httpMethod, patterns, cacheo) = httpMethod_patterns_cacheo
        for (p <- patterns) buffer.append((httpMethod, p, actionClass, cacheo))
      }
    }

    buffer.toArray
  }

  def supportedAnnotations = Array(
    classOf[GET].getName, classOf[GETs].getName,
    classOf[POST].getName,
    classOf[PUT].getName,
    classOf[DELETE].getName,
    classOf[CacheActionDay].getName, classOf[CacheActionHour].getName, classOf[CacheActionMinute].getName, classOf[CacheActionSecond].getName,
    classOf[CachePageDay].getName,   classOf[CachePageDay].getName,    classOf[CachePageDay].getName,      classOf[CachePageDay].getName)

  def discovered(className: String, _annotationName: String) {
    val klass = Class.forName(className).asInstanceOf[Class[Action]]
    if (firsts.contains(klass) || lasts.contains(klass) || others.contains(klass)) return

    // Annovention limitation: The annotations must be set on method, not class!
    val annotations = klass.getAnnotations
    collectRoute(annotations) match {
      case None =>

      case Some((routeMap, httpMethod, routePatterns)) =>
        val cacheo = collectCache(annotations)
        routeMap(klass) = (httpMethod, routePatterns, cacheo)
    }
  }

  //----------------------------------------------------------------------------

  private def collectRoute(annotations: Array[JAnnotation]): Option[(RouteMap, HttpMethod, Array[Routes.Pattern])] = {
    var ret: Option[(RouteMap, HttpMethod, Array[Routes.Pattern])] = None
    for (a <- annotations) {
      if (a.isInstanceOf[GET]) {
        val a2   = a.asInstanceOf[GET]
        val coll = if (a2.first) firsts else if (a2.last) lasts else others
        ret      = Some((coll, HttpMethod.GET, Array(a2.value)))
      } else if (a.isInstanceOf[GETs]) {
        val a2   = a.asInstanceOf[GETs]
        val coll = if (a2.first) firsts else if (a2.last) lasts else others
        ret      = Some((coll, HttpMethod.GET, a2.value))
      }

      else if (a.isInstanceOf[POST]) {
        val a2 = a.asInstanceOf[POST]
        ret    = Some((others, HttpMethod.POST, Array(a2.value)))
      }

      else if (a.isInstanceOf[PUT]) {
        val a2 = a.asInstanceOf[PUT]
        ret    = Some((others, HttpMethod.PUT, Array(a2.value)))
      } else if (a.isInstanceOf[DELETE]) {
        val a2 = a.asInstanceOf[DELETE]
        ret    = Some((others, HttpMethod.DELETE, Array(a2.value)))
      }
    }
    ret
  }

  private def collectCache(annotations: Array[JAnnotation]): Option[Cache] = {
    var ret: Option[Cache] = None
    for (a <- annotations) {
      if (a.isInstanceOf[CacheActionDay]) {
        val a2 = a.asInstanceOf[CacheActionDay]
        ret    = Some(new CacheAction(a2.value * 24 * 60 * 60))
      } else if (a.isInstanceOf[CacheActionHour]) {
        val a2 = a.asInstanceOf[CacheActionHour]
        ret    = Some(new CacheAction(a2.value      * 60 * 60))
      } else if (a.isInstanceOf[CacheActionMinute]) {
        val a2 = a.asInstanceOf[CacheActionMinute]
        ret    = Some(new CacheAction(a2.value           * 60))
      } else if (a.isInstanceOf[CacheActionSecond]) {
        val a2 = a.asInstanceOf[CacheActionSecond]
        ret    = Some(new CacheAction(a2.value))
      }

      else if (a.isInstanceOf[CachePageDay]) {
        val a2 = a.asInstanceOf[CachePageDay]
        ret    = Some(new CachePage(a2.value * 24 * 60 * 60))
      } else if (a.isInstanceOf[CachePageHour]) {
        val a2 = a.asInstanceOf[CachePageHour]
        ret    = Some(new CachePage(a2.value      * 60 * 60))
      } else if (a.isInstanceOf[CachePageMinute]) {
        val a2 = a.asInstanceOf[CachePageMinute]
        ret    = Some(new CachePage(a2.value           * 60))
      } else if (a.isInstanceOf[CachePageSecond]) {
        val a2 = a.asInstanceOf[CachePageSecond]
        ret    = Some(new CachePage(a2.value))
      }
    }
    ret
  }
}
