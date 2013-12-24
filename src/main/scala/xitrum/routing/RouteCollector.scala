package xitrum.routing

import scala.collection.mutable.{ArrayBuffer, Map => MMap}
import scala.reflect.runtime.universe
import scala.util.control.NonFatal

import org.objectweb.asm.{AnnotationVisitor, ClassReader, ClassVisitor, Opcodes, Type}
import sclasner.{FileEntry, Scanner}

import xitrum.{Action, Log, SockJsActor}
import xitrum.annotation._
import xitrum.sockjs.SockJsPrefix

case class DiscoveredAcc(
  normalRoutes:              SerializableRouteCollection,
  sockJsWithoutPrefixRoutes: SerializableRouteCollection,
  sockJsMap:                 Map[String, SockJsClassAndOptions],
  swaggerMap:                Map[Class[_ <: Action], Swagger]
)

/** Scan all classes to collect routes from actions. */
class RouteCollector extends Log {
  import ActionAnnotations._

  def deserializeCacheFileOrRecollect(cachedFileName: String): DiscoveredAcc = {
    var acc = DiscoveredAcc(
      new SerializableRouteCollection,
      new SerializableRouteCollection,
      Map.empty[String, SockJsClassAndOptions],
      Map.empty[Class[_ <: Action], Swagger]
    )

    val actionTreeBuilder = Scanner.foldLeft(cachedFileName, new ActionTreeBuilder, discovered _)
    val ka                = actionTreeBuilder.getConcreteActionsAndAnnotations
    ka.foreach { case (klass, annotations) =>
      acc = processAnnotations(acc, klass, annotations)
    }

    acc
  }

  //----------------------------------------------------------------------------

  private def discovered(acc: ActionTreeBuilder, entry: FileEntry): ActionTreeBuilder = {
    try {
      if (entry.relPath.endsWith(".class")) {
        val reader = new ClassReader(entry.bytes)
        acc.addBranches(reader.getClassName, reader.getSuperName, reader.getInterfaces)
      } else {
        acc
      }
    } catch {
      case NonFatal(e) =>
        log.warn("Could not scan route for " + entry.relPath + " in " + entry.container, e)
        acc
    }
  }

  private def processAnnotations(
      acc:         DiscoveredAcc,
      klass:       Class[_ <: Action],
      annotations: ActionAnnotations
  ): DiscoveredAcc = {
    val className  = klass.getName
    val fromSockJs = className.startsWith(classOf[SockJsPrefix].getPackage.getName)
    val routes     = if (fromSockJs) acc.sockJsWithoutPrefixRoutes else acc.normalRoutes

    collectNormalRoutes(routes, className, annotations)
    val newSockJsMap = collectSockJsMap(acc.sockJsMap, className, annotations)
    collectErrorRoutes(routes, className, annotations)

    val newSwaggerMap = collectSwagger(annotations) match {
      case None          => acc.swaggerMap
      case Some(swagger) => acc.swaggerMap + (klass -> swagger)
    }

    DiscoveredAcc(acc.normalRoutes, acc.sockJsWithoutPrefixRoutes, newSockJsMap, newSwaggerMap)
  }

  private def collectNormalRoutes(
      routes:      SerializableRouteCollection,
      className:   String,
      annotations: ActionAnnotations
  ) {
    var routeOrder          = optRouteOrder(annotations.routeOrder)  // -1: first, 1: last, 0: other
    var cacheSecs           = optCacheSecs(annotations.cache)        // < 0: cache action, > 0: cache page, 0: no cache
    var method_pattern_coll = ArrayBuffer.empty[(String, String)]

    annotations.routes.foreach { a =>
      listMethodAndPattern(a).foreach { m_p   => method_pattern_coll.append(m_p) }
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

        case (-1, "PATCH") => routes.firstPATCHs
        case ( 1, "PATCH") => routes.lastPATCHs
        case ( 0, "PATCH") => routes.otherPATCHs

        case (-1, "DELETE") => routes.firstDELETEs
        case ( 1, "DELETE") => routes.lastDELETEs
        case ( 0, "DELETE") => routes.otherDELETEs

        case (-1, "WEBSOCKET") => routes.firstWEBSOCKETs
        case ( 1, "WEBSOCKET") => routes.lastWEBSOCKETs
        case ( 0, "WEBSOCKET") => routes.otherWEBSOCKETs
      }
      coll.append(serializableRoute)
    }
  }

  private def collectErrorRoutes(
      routes:      SerializableRouteCollection,
      className:   String,
      annotations: ActionAnnotations)
  {
    annotations.error.foreach { case a =>
      val tpe = a.tpe
      if (tpe == typeOfError404) routes.error404 = Some(className)
      if (tpe == typeOfError500) routes.error500 = Some(className)
    }
  }

  private def collectSockJsMap(
      sockJsMap:   Map[String, SockJsClassAndOptions],
      className:   String,
      annotations: ActionAnnotations
  ): Map[String, SockJsClassAndOptions] =
  {
    var pathPrefix: String = null
    annotations.routes.foreach { case a =>
      if (a.tpe == typeOfSOCKJS)
        pathPrefix = a.scalaArgs(0).productElement(0).asInstanceOf[universe.Constant].value.toString
    }

    if (pathPrefix == null) {
      sockJsMap
    } else {
      val klass        = Class.forName(className).asInstanceOf[Class[SockJsActor]]
      val noWebSocket  = annotations.sockJsNoWebSocket.isDefined
      val cookieNeeded = annotations.sockJsCookieNeeded.isDefined
      sockJsMap + (pathPrefix -> new SockJsClassAndOptions(klass, !noWebSocket, cookieNeeded))
    }
  }

  //----------------------------------------------------------------------------

  /** -1: first, 1: last, 0: other */
  private def optRouteOrder(annotationo: Option[universe.Annotation]): Int = {
    annotationo match {
      case None => 0

      case Some(annotation) =>
        val tpe = annotation.tpe
        if (tpe == typeOfFirst)
          -1
        else if (tpe == typeOfLast)
          1
        else
          0
    }
  }

  /** < 0: cache action, > 0: cache page, 0: no cache */
  private def optCacheSecs(annotationo: Option[universe.Annotation]): Int = {
    annotationo match {
      case None => 0

      case Some(annotation) =>
        val tpe = annotation.tpe

        if (tpe == typeOfCacheActionDay)
          -annotation.scalaArgs(0).toString.toInt * 24 * 60 * 60
        else if (tpe == typeOfCacheActionHour)
          -annotation.scalaArgs(0).toString.toInt      * 60 * 60
        else if (tpe == typeOfCacheActionMinute)
          -annotation.scalaArgs(0).toString.toInt           * 60
        else if (tpe == typeOfCacheActionSecond)
          -annotation.scalaArgs(0).toString.toInt
        else if (tpe == typeOfCachePageDay)
          annotation.scalaArgs(0).toString.toInt * 24 * 60 * 60
        else if (tpe == typeOfCachePageHour)
          annotation.scalaArgs(0).toString.toInt      * 60 * 60
        else if (tpe == typeOfCachePageMinute)
          annotation.scalaArgs(0).toString.toInt           * 60
        else if (tpe == typeOfCachePageSecond)
          annotation.scalaArgs(0).toString.toInt
        else
          0
    }
  }

  /** @return Seq[(method, pattern)] */
  private def listMethodAndPattern(annotation: universe.Annotation): Seq[(String, String)] = {
    val tpe = annotation.tpe

    if (tpe == typeOfGET)
      getStrings(annotation).map(("GET", _))
    else if (tpe == typeOfPOST)
      getStrings(annotation).map(("POST", _))
    else if (tpe == typeOfPUT)
      getStrings(annotation).map(("PUT", _))
    else if (tpe == typeOfPATCH)
      getStrings(annotation).map(("PATCH", _))
    else if (tpe == typeOfDELETE)
      getStrings(annotation).map(("DELETE", _))
    else if (tpe == typeOfWEBSOCKET)
      getStrings(annotation).map(("WEBSOCKET", _))
    else
      Seq()
  }

  private def getStrings(annotation: universe.Annotation): Seq[String] = {
    annotation.scalaArgs.map { tree => tree.productElement(0).asInstanceOf[universe.Constant].value.toString }
  }

  //----------------------------------------------------------------------------

  private def collectSwagger(annotations: ActionAnnotations): Option[Swagger] = {
    val universeAnnotations = annotations.swaggers
    if (universeAnnotations.isEmpty) {
      None
    } else {
      var swaggerArgs = Seq.empty[SwaggerArg]
      universeAnnotations.foreach { annotation =>
        annotation.scalaArgs.foreach { scalaArg =>
          // Ex:
          // List(xitrum.annotation.Swagger.Response.apply, 200, "ID of the newly created article will be returned")
          // List(xitrum.annotation.Swagger.StringForm.apply, "title", xitrum.annotation.Swagger.StringForm.apply$default$2)
          // List(xitrum.annotation.Swagger.StringForm.apply, "title", "desc")
          val children = scalaArg.children

          val child0 = children(0).toString
          if (child0 == "xitrum.annotation.Swagger.Summary.apply") {
            val summary = children(1).productElement(0).asInstanceOf[universe.Constant].value.toString
            swaggerArgs = swaggerArgs :+ Swagger.Summary(summary)
          } else if (child0 == "xitrum.annotation.Swagger.Note.apply") {
            val note = children(1).productElement(0).asInstanceOf[universe.Constant].value.toString
            swaggerArgs = swaggerArgs :+ Swagger.Note(note)
          } else if (child0 == "xitrum.annotation.Swagger.Response.apply") {
            val code = children(1).toString.toInt
            val desc = children(2).productElement(0).asInstanceOf[universe.Constant].value.toString
            swaggerArgs = swaggerArgs :+ Swagger.Response(code, desc)
          } else {  // param or optional param
            val name = children(1).productElement(0).asInstanceOf[universe.Constant].value.toString

            val desc =
              if (children(2).toString.startsWith("xitrum.annotation.Swagger"))
                ""
              else
                children(2).productElement(0).asInstanceOf[universe.Constant].value.toString

            // Use reflection to create annotation

            // Ex: xitrum.annotation.Swagger.StringForm.apply
            val scalaClassName = child0.substring(0, child0.length - ".apply".length)

            val builder = new StringBuilder(scalaClassName)
            builder.setCharAt("xitrum.annotation.Swagger".length, '$')

            // Ex: xitrum.annotation.Swagger$StringForm
            val javaClassName = builder.toString
            val klass         = Class.forName(javaClassName)
            val constructor   = klass.getConstructor(classOf[String], classOf[String])
            swaggerArgs = swaggerArgs :+ constructor.newInstance(name, desc).asInstanceOf[SwaggerArg]
          }
        }
      }

      Some(Swagger(swaggerArgs: _*))
    }
  }
}
