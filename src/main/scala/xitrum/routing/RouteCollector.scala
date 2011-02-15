package xitrum.routing

import scala.collection.mutable.{ArrayBuffer, HashMap => MHashMap}

import org.jboss.netty.handler.codec.http.HttpMethod

import com.impetus.annovention.{Discoverer, ClasspathDiscoverer}
import com.impetus.annovention.listener.ClassAnnotationDiscoveryListener

import xitrum._

/** Scan all classes to collect routes. */
class RouteCollector extends ClassAnnotationDiscoveryListener {
  private val firsts = new MHashMap[Class[Action], (HttpMethod, Array[Routes.Pattern])]
  private val lasts  = new MHashMap[Class[Action], (HttpMethod, Array[Routes.Pattern])]
  private val others = new MHashMap[Class[Action], (HttpMethod, Array[Routes.Pattern])]

  def collect: Array[Routes.Route]  = {
    val discoverer = new ClasspathDiscoverer
    discoverer.addAnnotationListener(this)
    discoverer.discover

    val buffer = new ArrayBuffer[Routes.Route]
    for (map <- Array(firsts, others, lasts)) {
      val sorted = map.toBuffer.sortWith { (a1, a2) =>
        a1.toString < a2.toString
      }

      for ((actionClass, httpMethod_patterns) <- sorted) {
        val (httpMethod, patterns) = httpMethod_patterns
        for (p <- patterns) buffer.append((httpMethod, p, actionClass))
      }
    }
    buffer.toArray
  }

  def supportedAnnotations = Array(
    classOf[GET].getName, classOf[GETs].getName,
    classOf[POST].getName,
    classOf[PUT].getName,
    classOf[DELETE].getName)

  def discovered(className: String, _annotationName: String) {
    val klass = Class.forName(className).asInstanceOf[Class[Action]]
    if (firsts.contains(klass) || lasts.contains(klass) || others.contains(klass)) return

    // Annovention limitation: The annotations must be set on method, not class!
    val annotations = klass.getAnnotations
    for (a <- annotations) {
      if (a.isInstanceOf[GET]) {
        val a2      = a.asInstanceOf[GET]
        val coll    = if (a2.first) firsts else if (a2.last) lasts else others
        coll(klass) = (HttpMethod.GET, Array(a2.value))
      } else if (a.isInstanceOf[GETs]) {
        val a2      = a.asInstanceOf[GETs]
        val coll    = if (a2.first) firsts else if (a2.last) lasts else others
        coll(klass) = (HttpMethod.GET, a2.value)
      }

      else if (a.isInstanceOf[POST]) {
        val a2        = a.asInstanceOf[POST]
        others(klass) = (HttpMethod.POST, Array(a2.value))
      }

      else if (a.isInstanceOf[PUT]) {
        val a2        = a.asInstanceOf[PUT]
        others(klass) = (HttpMethod.PUT, Array(a2.value))
      } else if (a.isInstanceOf[DELETE]) {
        val a2        = a.asInstanceOf[DELETE]
        others(klass) = (HttpMethod.DELETE, Array(a2.value))
      }
    }
  }
}
