package xitrum.routing

import java.io.{ByteArrayInputStream, DataInputStream, File}
import scala.collection.mutable.{ArrayBuffer, Map => MMap}

import javassist.bytecode.ClassFile
import javassist.bytecode.AnnotationsAttribute
import javassist.bytecode.annotation.{Annotation, MemberValue, ArrayMemberValue, StringMemberValue, IntegerMemberValue}

import io.netty.handler.codec.http.HttpMethod

import sclasner.{FileEntry, Scanner}

import xitrum.{Action, Config, Logger}
import xitrum.annotation._

/** Scan all classes to collect routes. */
class RouteCollector extends Logger {
  // Use String because HttpMethod is not serializable
  type RouteMap = Map[Class[Action], (String, Array[Routes.Pattern], Int)]

  object RouteOrder extends Enumeration {
    type RouteOrder = Value
    val firsts, lasts, others = Value
  }

  def collect: (Array[Routes.Route], Map[Class[Action], Int]) = {
    logger.info("Collect routes/load routes.sclasner...")

    val routeBuffer = ArrayBuffer[Routes.Route]()
    val cacheBuffer = MMap[Class[Action], Int]()

    val empty = Map[Class[Action], (String, Array[Routes.Pattern], Int)]()
    val (firsts, lasts, others) = try {
      Scanner.foldLeft("routes.sclasner", (empty, empty, empty), discovered _)
    } catch {
      case e =>
        // Maybe routes.sclasner could not be loaded because dependencies have changed.
        // Try deleting routes.sclasner and scan again.
        val f = new File("routes.sclasner")
        if (f.exists) {
          logger.warn("Error loading routes.sclasner. Delete the file and recollect routes...")
          f.delete
          try {
            Scanner.foldLeft("routes.sclasner", (empty, empty, empty), discovered _)
          } catch {
            case e2 =>
              Config.exitOnError("Could not collect routes", e2)
              throw e2
          }
        } else {
          Config.exitOnError("Could not collect routes", e)
          throw e
        }
    }

    // Make PostbackAction the first route for quicker route matching
    routeBuffer.append((HttpMethod.POST, PostbackAction.POSTBACK_PREFIX + ":*", classOf[PostbackAction].asInstanceOf[Class[Action]]))

    for (map <- Array(firsts, others, lasts)) {
      val sorted = map.toBuffer.sortWith { (a1, a2) =>
        a1.toString < a2.toString
      }

      for ((actionClass, httpMethod_patterns_cacheSecs) <- sorted) {
        val (httpMethod, patterns, cacheSecs) = httpMethod_patterns_cacheSecs
        for (p <- patterns) routeBuffer.append((new HttpMethod(httpMethod), p, actionClass))
        cacheBuffer(actionClass) = cacheSecs
      }
    }

    (routeBuffer.toArray, cacheBuffer.toMap)
  }

  //----------------------------------------------------------------------------

  private def discovered(acc: (RouteMap, RouteMap, RouteMap), entry: FileEntry) = {
    try {
      if (entry.relPath.endsWith(".class")) {
        val bais = new ByteArrayInputStream(entry.bytes)
        val dis  = new DataInputStream(bais)
        val cf   = new ClassFile(dis)
        dis.close

        val aa = cf.getAttribute(AnnotationsAttribute.visibleTag).asInstanceOf[AnnotationsAttribute]
        if (aa == null) {
          acc
        } else {
          val as = aa.getAnnotations
          collectRoute(as) match {
            case None => acc

            case Some((order, httpMethod, routePatterns)) =>
              val (firsts, lasts, others) = acc
              val klass                   = Class.forName(cf.getName).asInstanceOf[Class[Action]]
              val cacheSecs               = collectCache(as)
              val route                   = (klass -> (httpMethod, routePatterns, cacheSecs))
              order match {
                case RouteOrder.firsts => (firsts + route, lasts, others)
                case RouteOrder.lasts  => (firsts, lasts + route, others)
                case RouteOrder.others => (firsts, lasts, others + route)
              }
          }
        }
      } else {
        acc
      }
    } catch {
      case e =>
        logger.debug("Could not scan route for " + entry.relPath + " in " + entry.container, e)
        acc
    }
  }

  private def collectRoute(as: Array[Annotation]): Option[(RouteOrder.RouteOrder, String, Array[Routes.Pattern])] = {
    var order                           = RouteOrder.others
    var method:   String                = null
    var patterns: Array[Routes.Pattern] = null

    as.foreach { a =>
      val tn = a.getTypeName

      if (tn == classOf[First].getName) {
        order = RouteOrder.firsts
      } else if (tn == classOf[Last].getName) {
        order = RouteOrder.lasts
      }

      else if (tn == classOf[GET].getName) {
        method   = "GET"
        patterns = getMethodPattern(a)
      } else if (tn == classOf[GETs].getName) {
        method   = "GET"
        patterns = getMethodPatterns(a)
      }

      else if (tn == classOf[POST].getName) {
        method   = "POST"
        patterns = getMethodPattern(a)
      } else if (tn == classOf[POSTs].getName) {
        method   = "POST"
        patterns = getMethodPatterns(a)
      }

      else if (tn == classOf[PUT].getName) {
        method   = "PUT"
        patterns = getMethodPattern(a)
      } else if (tn == classOf[PUTs].getName) {
        method   = "PUT"
        patterns = getMethodPatterns(a)
      }

      else if (tn == classOf[DELETE].getName) {
        method   = "DELETE"
        patterns = getMethodPattern(a)
      } else if (tn == classOf[DELETEs].getName) {
        method   = "DELETE"
        patterns = getMethodPatterns(a)
      }

      else if (tn == classOf[WEBSOCKET].getName) {
        method   = "WEBSOCKET"
        patterns = getMethodPattern(a)
      } else if (tn == classOf[WEBSOCKETs].getName) {
        method   = "WEBSOCKET"
        patterns = getMethodPatterns(a)
      }
    }

    if (method != null && patterns != null) Some(order, method, patterns) else None
  }

  private def getMethodPattern(a: Annotation): Array[String] =
    Array(a.getMemberValue("value").asInstanceOf[StringMemberValue].getValue)

  private def getMethodPatterns(a: Annotation): Array[String] =
    a.getMemberValue("value").asInstanceOf[ArrayMemberValue].getValue.map(_.asInstanceOf[StringMemberValue].getValue)

  //----------------------------------------------------------------------------

  private def collectCache(as: Array[Annotation]): Int = {
    as.foreach { a =>
      val tn = a.getTypeName

      val secs =
           if (tn == classOf[CacheActionDay].getName)
        - getCacheSecs(a) * 24 * 60 * 60
      else if (tn == classOf[CacheActionHour].getName)
        - getCacheSecs(a)      * 60 * 60
      else if (tn == classOf[CacheActionMinute].getName)
        - getCacheSecs(a)           * 60
      else if (tn == classOf[CacheActionSecond].getName)
        - getCacheSecs(a)

      else if (tn == classOf[CachePageDay].getName)
        getCacheSecs(a) * 24 * 60 * 60
      else if (tn == classOf[CachePageHour].getName)
        getCacheSecs(a)      * 60 * 60
      else if (tn == classOf[CachePageMinute].getName)
        getCacheSecs(a)           * 60
      else if (tn == classOf[CachePageSecond].getName)
        getCacheSecs(a)
      else
        0

      if (secs != 0) return secs
    }
    0
  }

  private def getCacheSecs(a: Annotation) =
    a.getMemberValue("value").asInstanceOf[IntegerMemberValue].getValue
}
