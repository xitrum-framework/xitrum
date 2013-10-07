package xitrum.annotation

import scala.reflect.runtime.universe

// http://docs.scala-lang.org/overviews/reflection/annotations-names-scopes.html
// http://www.veebsbraindump.com/2013/01/reflecting-annotations-in-scala-2-10/

object ActionAnnotations {
  val typeOfRoute              = universe.typeOf[Route]
  val typeOfRouteOrder         = universe.typeOf[RouteOrder]
  val typeOfSockJsCookieNeeded = universe.typeOf[SockJsCookieNeeded]
  val typeOfSockJsNoWebSocket  = universe.typeOf[SockJsNoWebSocket]
  val typeOfError              = universe.typeOf[Error]
  val typeOfCache              = universe.typeOf[Cache]

  val typeOfSwagger = universe.typeOf[Swagger]

  /* Do not use the below to avoid Scala reflection error:
java.lang.RuntimeException: error reading Scala signature of xitrum.annotation.Swagger: 61435
        at scala.reflect.internal.pickling.UnPickler.unpickle(UnPickler.scala:45)
        at scala.reflect.runtime.JavaMirrors$JavaMirror.unpickleClass(JavaMirrors.scala:574)
        at scala.reflect.runtime.SymbolLoaders$TopClassCompleter.complete(SymbolLoaders.scala:31)
        at scala.reflect.internal.Symbols$Symbol.info(Symbols.scala:1217)
        at scala.reflect.internal.pickling.UnPickler$Scan.scala$reflect$internal$pickling$UnPickler$Scan$$fromName$1(UnPickler.scala:207)
        at scala.reflect.internal.pickling.UnPickler$Scan.readExtSymbol$1(UnPickler.scala:226)
        at scala.reflect.internal.pickling.UnPickler$Scan.readSymbol(UnPickler.scala:250)
        at scala.reflect.internal.pickling.UnPickler$Scan.readSymbolRef(UnPickler.scala:783)
        at scala.reflect.internal.pickling.UnPickler$Scan.readType(UnPickler.scala:346)
        at scala.reflect.internal.pickling.UnPickler$Scan$$anonfun$readTypeRef$1.apply(UnPickler.scala:792)
        at scala.reflect.internal.pickling.UnPickler$Scan$$anonfun$readTypeRef$1.apply(UnPickler.scala:792)
        at scala.reflect.internal.pickling.UnPickler$Scan.at(UnPickler.scala:171)
        at scala.reflect.internal.pickling.UnPickler$Scan.readTypeRef(UnPickler.scala:792)
        at scala.reflect.internal.pickling.UnPickler$Scan.readTree(UnPickler.scala:511)
        at scala.reflect.internal.pickling.UnPickler$Scan$$anonfun$readAnnotArg$1.apply(UnPickler.scala:443)
        at scala.reflect.internal.pickling.UnPickler$Scan$$anonfun$readAnnotArg$1.apply(UnPickler.scala:443)
        at scala.reflect.internal.pickling.UnPickler$Scan.at(UnPickler.scala:171)
        at scala.reflect.internal.pickling.UnPickler$Scan.readAnnotArg(UnPickler.scala:443)
        at scala.reflect.internal.pickling.UnPickler$Scan.readAnnotationInfo(UnPickler.scala:477)
        at scala.reflect.internal.pickling.UnPickler$Scan.readSymbolAnnotation(UnPickler.scala:491)
        at scala.reflect.internal.pickling.UnPickler$Scan.run(UnPickler.scala:88)
        at scala.reflect.internal.pickling.UnPickler.unpickle(UnPickler.scala:37)
        at scala.reflect.runtime.JavaMirrors$JavaMirror.unpickleClass(JavaMirrors.scala:561)
        at scala.reflect.runtime.SymbolLoaders$TopClassCompleter.complete(SymbolLoaders.scala:31)
        at scala.reflect.internal.Symbols$Symbol.info(Symbols.scala:1217)
        at scala.reflect.internal.Symbols$Symbol.initialize(Symbols.scala:1352)
        at scala.reflect.internal.Symbols$Symbol.annotations(Symbols.scala:1559)
*/
  //val typeOfSwaggerResponse = universe.typeOf[Swagger.Response]

  val typeOfError404 = universe.typeOf[Error404]
  val typeOfError500 = universe.typeOf[Error500]

  val typeOfGET       = universe.typeOf[GET]
  val typeOfPOST      = universe.typeOf[POST]
  val typeOfPUT       = universe.typeOf[PUT]
  val typeOfPATCH     = universe.typeOf[PATCH]
  val typeOfDELETE    = universe.typeOf[DELETE]
  val typeOfOPTIONS   = universe.typeOf[OPTIONS]
  val typeOfWEBSOCKET = universe.typeOf[WEBSOCKET]
  val typeOfSOCKJS    = universe.typeOf[SOCKJS]

  val typeOfFirst = universe.typeOf[First]
  val typeOfLast  = universe.typeOf[Last]

  val typeOfCacheActionDay    = universe.typeOf[CacheActionDay]
  val typeOfCacheActionHour   = universe.typeOf[CacheActionHour]
  val typeOfCacheActionMinute = universe.typeOf[CacheActionMinute]
  val typeOfCacheActionSecond = universe.typeOf[CacheActionSecond]

  val typeOfCachePageDay    = universe.typeOf[CachePageDay]
  val typeOfCachePageHour   = universe.typeOf[CachePageHour]
  val typeOfCachePageMinute = universe.typeOf[CachePageMinute]
  val typeOfCachePageSecond = universe.typeOf[CachePageSecond]
}

case class ActionAnnotations(
  route:      Option[universe.Annotation] = None,
  routeOrder: Option[universe.Annotation] = None,

  sockJsCookieNeeded: Option[universe.Annotation] = None,
  sockJsNoWebSocket:  Option[universe.Annotation] = None,

  error: Option[universe.Annotation] = None,

  cache: Option[universe.Annotation] = None,

  swagger: Option[universe.Annotation] = None
) {
  import ActionAnnotations._

  def overrideMe(other: ActionAnnotations) = ActionAnnotations(
    other.route              orElse route,
    other.routeOrder         orElse routeOrder,
    other.sockJsCookieNeeded orElse sockJsCookieNeeded,
    other.sockJsNoWebSocket  orElse sockJsNoWebSocket,
    other.error              orElse error,
    other.cache              orElse cache,
    other.swagger            orElse swagger
  )

  def overrideMe(annotations: Seq[universe.Annotation]): ActionAnnotations = {
    var ret = this
    annotations.foreach { a =>
      if (a.tpe <:< typeOfRoute)
        ret = ret.copy(route = Some(a))

      else if (a.tpe <:< typeOfRouteOrder)
        ret = ret.copy(routeOrder = Some(a))

      else if (a.tpe <:< typeOfSockJsCookieNeeded)
        ret = ret.copy(sockJsCookieNeeded = Some(a))

      else if (a.tpe <:< typeOfSockJsNoWebSocket)
        ret = ret.copy(sockJsNoWebSocket = Some(a))

      else if (a.tpe <:< typeOfError)
        ret = ret.copy(error = Some(a))

      else if (a.tpe <:< typeOfCache)
        ret = ret.copy(cache = Some(a))

      else if (a.tpe <:< typeOfSwagger)
        ret = ret.copy(swagger = Some(a))
    }
    ret
  }
}
