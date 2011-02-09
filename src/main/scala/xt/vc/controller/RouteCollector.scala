package xt.vc.controller

import java.lang.reflect.Method
import java.net.URL
import java.net.URLClassLoader

import scala.collection.mutable.{ArrayBuffer, HashMap => MHashMap}

import org.jboss.netty.handler.codec.http.HttpMethod

import com.impetus.annovention.{Discoverer, ClasspathDiscoverer}
import com.impetus.annovention.listener.MethodAnnotationDiscoveryListener

import xt._

/** Scan all classes to collect routes. */
class RouteCollector extends MethodAnnotationDiscoveryListener {
  //                                 controller         action                paths
  private val firsts = new MHashMap[(Class[Controller], Method), (HttpMethod, Array[String])]
  private val lasts  = new MHashMap[(Class[Controller], Method), (HttpMethod, Array[String])]
  private val others = new MHashMap[(Class[Controller], Method), (HttpMethod, Array[String])]

  def collect: Array[Routes.Route]  = {
    val discoverer = new ClasspathDiscoverer
    discoverer.addAnnotationListener(this)
    discoverer.discover

    val buffer = new ArrayBuffer[Routes.Route]
    for (map <- Array(firsts, others, lasts)) {
      // Sort routes by controller then action
      val sorted = map.toBuffer.sortWith { (e1, e2) =>
        val (ca1, mp1) = e1
        val (ca2, mp2) = e2

        val (c1, a1) = ca1
        val (c2, a2) = ca2

        val cs1 = c1.toString
        val cs2 = c2.toString
        val as1 = a1.toString
        val as2 = a2.toString

        if (cs1 == cs2) as1 < as2 else cs1 < cs2
      }

      for ((key, value) <- sorted) {
        val (httpMethod, paths) = value
        for (p <- paths) buffer.append((httpMethod, p, key))
      }
    }
    buffer.toArray
  }

  def supportedAnnotations = Array(
    classOf[GET].getName,    classOf[GETs].getName,
    classOf[POST].getName,   classOf[POSTs].getName,
    classOf[PUT].getName,    classOf[PUTs].getName,
    classOf[DELETE].getName, classOf[DELETEs].getName)

  def discovered(className: String, methodName: String, _annotationName: String) {
    val klass  = Class.forName(className).asInstanceOf[Class[Controller]]
    val method = klass.getMethod(methodName)
    val key    = (klass, method)

    if (firsts.contains(key) || lasts.contains(key) || others.contains(key)) return

    val annotations = method.getAnnotations
    for (a <- annotations) {
      if (a.isInstanceOf[GET]) {
        val a2    = a.asInstanceOf[GET]
        val coll  = if (a2.first) firsts else if (a2.first) lasts else others
        coll(key) = (HttpMethod.GET, Array(a2.value))
      } else if (a.isInstanceOf[GETs]) {
        val a2    = a.asInstanceOf[GETs]
        val coll  = if (a2.first) firsts else if (a2.first) lasts else others
        coll(key) = (HttpMethod.GET, a2.value)
      }

      else if (a.isInstanceOf[POST]) {
        val a2    = a.asInstanceOf[POST]
        val coll  = if (a2.first) firsts else if (a2.first) lasts else others
        coll(key) = (HttpMethod.POST, Array(a2.value))
      } else if (a.isInstanceOf[POSTs]) {
        val a2    = a.asInstanceOf[POSTs]
        val coll  = if (a2.first) firsts else if (a2.first) lasts else others
        coll(key) = (HttpMethod.POST, a2.value)
      }

      else if (a.isInstanceOf[PUT]) {
        val a2    = a.asInstanceOf[PUT]
        val coll  = if (a2.first) firsts else if (a2.first) lasts else others
        coll(key) = (HttpMethod.PUT, Array(a2.value))
      } else if (a.isInstanceOf[PUTs]) {
        val a2    = a.asInstanceOf[PUTs]
        val coll  = if (a2.first) firsts else if (a2.first) lasts else others
        coll(key) = (HttpMethod.PUT, a2.value)
      }

      else if (a.isInstanceOf[DELETE]) {
        val a2    = a.asInstanceOf[DELETE]
        val coll  = if (a2.first) firsts else if (a2.first) lasts else others
        coll(key) = (HttpMethod.DELETE, Array(a2.value))
      } else if (a.isInstanceOf[DELETEs]) {
        val a2    = a.asInstanceOf[DELETEs]
        val coll  = if (a2.first) firsts else if (a2.first) lasts else others
        coll(key) = (HttpMethod.DELETE, a2.value)
      }
    }
  }
}
