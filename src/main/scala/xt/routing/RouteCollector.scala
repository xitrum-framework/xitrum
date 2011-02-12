package xt.routing

import scala.collection.mutable.{ArrayBuffer, HashMap => MHashMap}

import org.jboss.netty.handler.codec.http.HttpMethod

import com.impetus.annovention.{Discoverer, ClasspathDiscoverer}
import com.impetus.annovention.listener.MethodAnnotationDiscoveryListener

import xt._

/** Scan all classes to collect routes. */
class RouteCollector extends MethodAnnotationDiscoveryListener {
  private val firsts = new MHashMap[(Class[Action]), (HttpMethod, Array[Routes.Pattern])]
  private val lasts  = new MHashMap[(Class[Action]), (HttpMethod, Array[Routes.Pattern])]
  private val others = new MHashMap[(Class[Action]), (HttpMethod, Array[Routes.Pattern])]

  def collect: Array[Routes.Route]  = {
    val discoverer = new ClasspathDiscoverer
    discoverer.addAnnotationListener(this)
    discoverer.discover

    val buffer = new ArrayBuffer[Routes.Route]
    for (map <- Array(firsts, others, lasts)) {
      val sorted = map.toBuffer.sortWith { (a1, a2) =>
        a1.toString < a2.toString
      }

      for ((key, value) <- sorted) {
        val (httpMethod, patterns) = value
        for (p <- patterns) buffer.append((httpMethod, p, key))
      }
    }
    buffer.toArray
  }

  def supportedAnnotations = Array(
    classOf[GET].getName,    classOf[GETs].getName,
    classOf[POST].getName,   classOf[POSTs].getName,
    classOf[PUT].getName,    classOf[PUTs].getName,
    classOf[DELETE].getName, classOf[DELETEs].getName)

  def discovered(className: String, _methodName: String, _annotationName: String) {
    val klass  = Class.forName(className).asInstanceOf[Class[Action]]

    if (firsts.contains(klass) || lasts.contains(klass) || others.contains(klass)) return

    val annotations = klass.getAnnotations
    for (a <- annotations) {
      if (a.isInstanceOf[GET]) {
        val a2    = a.asInstanceOf[GET]
        val coll  = if (a2.first) firsts else if (a2.first) lasts else others
        coll(klass) = (HttpMethod.GET, Array(a2.value))
      } else if (a.isInstanceOf[GETs]) {
        val a2    = a.asInstanceOf[GETs]
        val coll  = if (a2.first) firsts else if (a2.first) lasts else others
        coll(klass) = (HttpMethod.GET, a2.value)
      }

      else if (a.isInstanceOf[POST]) {
        val a2    = a.asInstanceOf[POST]
        val coll  = if (a2.first) firsts else if (a2.first) lasts else others
        coll(klass) = (HttpMethod.POST, Array(a2.value))
      } else if (a.isInstanceOf[POSTs]) {
        val a2    = a.asInstanceOf[POSTs]
        val coll  = if (a2.first) firsts else if (a2.first) lasts else others
        coll(klass) = (HttpMethod.POST, a2.value)
      }

      else if (a.isInstanceOf[PUT]) {
        val a2    = a.asInstanceOf[PUT]
        val coll  = if (a2.first) firsts else if (a2.first) lasts else others
        coll(klass) = (HttpMethod.PUT, Array(a2.value))
      } else if (a.isInstanceOf[PUTs]) {
        val a2    = a.asInstanceOf[PUTs]
        val coll  = if (a2.first) firsts else if (a2.first) lasts else others
        coll(klass) = (HttpMethod.PUT, a2.value)
      }

      else if (a.isInstanceOf[DELETE]) {
        val a2    = a.asInstanceOf[DELETE]
        val coll  = if (a2.first) firsts else if (a2.first) lasts else others
        coll(klass) = (HttpMethod.DELETE, Array(a2.value))
      } else if (a.isInstanceOf[DELETEs]) {
        val a2    = a.asInstanceOf[DELETEs]
        val coll  = if (a2.first) firsts else if (a2.first) lasts else others
        coll(klass) = (HttpMethod.DELETE, a2.value)
      }
    }
  }
}
