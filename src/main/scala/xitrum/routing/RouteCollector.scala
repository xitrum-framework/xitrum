package xitrum.routing

import java.lang.annotation.{Annotation => JAnnotation}
import scala.collection.mutable.{ArrayBuffer, Map => MMap}

import org.jboss.netty.handler.codec.http.HttpMethod

import com.impetus.annovention.{ClasspathDiscoverer, FilterImpl}
import com.impetus.annovention.listener.ClassAnnotationDiscoveryListener

import xitrum.Action
import xitrum.annotation._

/** Scan all classes to collect routes. */
class RouteCollector extends ClassAnnotationDiscoveryListener {
  type RouteMap = MMap[Class[Action], (HttpMethod, Array[Routes.Pattern], Int)]

  private val firsts = MMap[Class[Action], (HttpMethod, Array[Routes.Pattern], Int)]()
  private val lasts  = MMap[Class[Action], (HttpMethod, Array[Routes.Pattern], Int)]()
  private val others = MMap[Class[Action], (HttpMethod, Array[Routes.Pattern], Int)]()

  def collect: (Array[Routes.Route], Map[Class[Action], Int])  = {
    val ignoredPackages = FilterImpl.IGNORED_PACKAGES ++ Array("com.hazelcast", "com.codahale.jerkson", "org.slf4j", "ch.qos.logback")
    val filter = new FilterImpl(ignoredPackages)

    val discoverer = new ClasspathDiscoverer
    discoverer.addAnnotationListener(this)
    discoverer.setFilter(filter)
    discoverer.discover(true, false, false, true, false)

    val routeBuffer = ArrayBuffer[Routes.Route]()
    val cacheBuffer = MMap[Class[Action], Int]()

    // Make PostbackAction the first route for quicker route matching
    routeBuffer.append((HttpMethod.POST, PostbackAction.POSTBACK_PREFIX + ":*", classOf[PostbackAction].asInstanceOf[Class[Action]]))

    for (map <- Array(firsts, others, lasts)) {
      val sorted = map.toBuffer.sortWith { (a1, a2) =>
        a1.toString < a2.toString
      }

      for ((actionClass, httpMethod_patterns_cacheSecs) <- sorted) {
        val (httpMethod, patterns, cacheSecs) = httpMethod_patterns_cacheSecs
        for (p <- patterns) routeBuffer.append((httpMethod, p, actionClass))
        cacheBuffer(actionClass) = cacheSecs
      }
    }

    (routeBuffer.toArray, cacheBuffer.toMap)
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

    val processed = firsts.contains(klass) || lasts.contains(klass) || others.contains(klass)
    if (processed) return

    // Annovention limitation: The annotations must be set on method, not class!
    val annotations = klass.getAnnotations
    collectRoute(annotations) match {
      case None =>

      case Some((routeMap, httpMethod, routePatterns)) =>
        val cacheSecs = collectCache(annotations)
        routeMap(klass) = (httpMethod, routePatterns, cacheSecs)
    }
  }

  //----------------------------------------------------------------------------

  private def collectRoute(annotations: Array[JAnnotation]): Option[(RouteMap, HttpMethod, Array[Routes.Pattern])] = {
    var map:      RouteMap              = others
    var method:   HttpMethod            = null
    var patterns: Array[Routes.Pattern] = null

    annotations.foreach { a =>
      if (a.isInstanceOf[First]) {
        map = firsts
      } else if (a.isInstanceOf[Last]) {
        map = lasts
      }

      else if (a.isInstanceOf[GET]) {
        method   = HttpMethod.GET
        patterns = Array(a.asInstanceOf[GET].value)
      } else if (a.isInstanceOf[GETs]) {
        method   = HttpMethod.GET
        patterns = a.asInstanceOf[GETs].value
      }

      else if (a.isInstanceOf[POST]) {
        method   = HttpMethod.POST
        patterns = Array(a.asInstanceOf[POST].value)
      } else if (a.isInstanceOf[POSTs]) {
        method   = HttpMethod.POST
        patterns = a.asInstanceOf[POSTs].value
      }

      else if (a.isInstanceOf[PUT]) {
        method   = HttpMethod.PUT
        patterns = Array(a.asInstanceOf[PUT].value)
      } else if (a.isInstanceOf[PUTs]) {
        method   = HttpMethod.PUT
        patterns = a.asInstanceOf[PUTs].value
      }

      else if (a.isInstanceOf[DELETE]) {
        method   = HttpMethod.DELETE
        patterns = Array(a.asInstanceOf[DELETE].value)
      } else if (a.isInstanceOf[DELETEs]) {
        method   = HttpMethod.DELETE
        patterns = a.asInstanceOf[DELETEs].value
      }
    }

    if (method != null && patterns != null) Some(map, method, patterns) else None
  }

  private def collectCache(annotations: Array[JAnnotation]): Int = {
    var ret = 0
    for (a <- annotations) {
      if (a.isInstanceOf[CacheActionDay]) {
        val a2 = a.asInstanceOf[CacheActionDay]
        ret    = - a2.value * 24 * 60 * 60
      } else if (a.isInstanceOf[CacheActionHour]) {
        val a2 = a.asInstanceOf[CacheActionHour]
        ret    = - a2.value      * 60 * 60
      } else if (a.isInstanceOf[CacheActionMinute]) {
        val a2 = a.asInstanceOf[CacheActionMinute]
        ret    = - a2.value           * 60
      } else if (a.isInstanceOf[CacheActionSecond]) {
        val a2 = a.asInstanceOf[CacheActionSecond]
        ret    = - a2.value
      }

      else if (a.isInstanceOf[CachePageDay]) {
        val a2 = a.asInstanceOf[CachePageDay]
        ret    = a2.value * 24 * 60 * 60
      } else if (a.isInstanceOf[CachePageHour]) {
        val a2 = a.asInstanceOf[CachePageHour]
        ret    = a2.value      * 60 * 60
      } else if (a.isInstanceOf[CachePageMinute]) {
        val a2 = a.asInstanceOf[CachePageMinute]
        ret    = a2.value           * 60
      } else if (a.isInstanceOf[CachePageSecond]) {
        val a2 = a.asInstanceOf[CachePageSecond]
        ret    = a2.value
      }

      if (ret != 0) return ret
    }
    ret
  }
}
