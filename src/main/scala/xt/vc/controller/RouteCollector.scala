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
  //                                 controller         action    HTTP methods       paths
  private val firsts = new MHashMap[(Class[Controller], Method), (Array[HttpMethod], Array[String])]
  private val lasts  = new MHashMap[(Class[Controller], Method), (Array[HttpMethod], Array[String])]
  private val others = new MHashMap[(Class[Controller], Method), (Array[HttpMethod], Array[String])]

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
        val (httpMethods, paths) = value

        // paths is always non-empty, see "discovered" method below

        if (httpMethods.isEmpty) {
          for (p <- paths) buffer.append((None, p, key))
        } else {
          for (hm <- httpMethods; p <- paths) buffer.append((Some(hm), p, key))
        }
      }
    }
    buffer.toArray
  }

  def supportedAnnotations = Array(
    classOf[GET].getName,
    classOf[POST].getName,
    classOf[PUT].getName,
    classOf[DELETE].getName,
    classOf[Path].getName,
    classOf[Paths].getName)

  def discovered(className: String, methodName: String, _annotationName: String) {
    val klass  = Class.forName(className).asInstanceOf[Class[Controller]]
    val method = klass.getMethod(methodName)
    val key    = (klass, method)

    if (firsts.contains(key) || lasts.contains(key) || others.contains(key)) return

    val pathPrefix = {
      val pathAnnotation = klass.getAnnotation(classOf[Path])
      if (pathAnnotation != null) pathAnnotation.value else ""
    }

    val annotations = method.getAnnotations
    val httpMethods = new ArrayBuffer[HttpMethod]
    val paths       = new ArrayBuffer[String]
    var first       = false
    var last        = false
    for (annotation <- annotations) {
      if (annotation.isInstanceOf[Path]) {
        val pathAnnotation = annotation.asInstanceOf[Path]
        first = pathAnnotation.first
        last  = pathAnnotation.last
        paths.append(pathPrefix + pathAnnotation.value)
      } else if (annotation.isInstanceOf[Paths]) {
        val pathsAnnotation = annotation.asInstanceOf[Paths]
        first = pathsAnnotation.first
        last  = pathsAnnotation.last
        for (pv <- pathsAnnotation.value) paths.append(pathPrefix + pv)
      } else if (annotation.isInstanceOf[GET]) {
        httpMethods.append(HttpMethod.GET)
      } else if (annotation.isInstanceOf[POST]) {
        httpMethods.append(HttpMethod.POST)
      } else if (annotation.isInstanceOf[PUT]) {
        httpMethods.append(HttpMethod.PUT)
      } else if (annotation.isInstanceOf[DELETE]) {
        httpMethods.append(HttpMethod.DELETE)
      }
    }

    if (!paths.isEmpty) {
      if (first) {
        firsts(key) = (httpMethods.toArray, paths.toArray)
      } else if (last) {
        lasts(key)  = (httpMethods.toArray, paths.toArray)
      } else {
        others(key) = (httpMethods.toArray, paths.toArray)
      }
    }
  }
}
