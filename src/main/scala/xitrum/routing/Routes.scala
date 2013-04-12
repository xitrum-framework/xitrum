package xitrum.routing

import java.io.File

import scala.collection.mutable.{Map => MMap}
import scala.util.control.NonFatal

import akka.actor.{Actor, ActorRef, Props}
import com.esotericsoftware.reflectasm.ConstructorAccess

import xitrum.{Config, Logger, SockJsActor}

// "websocket" and "cookieNeeded" members are named after SockJS option:
// {"websocket": true/false, "cookie_needed": true/false, "origins": ["*:*"], "entropy": integer}
class SockJsClassAndOptions(val handlerClass: Class[_ <: SockJsActor], val websocket: Boolean, val cookieNeeded: Boolean)

object Routes extends Logger {
  private val ROUTES_CACHE = "routes.cache"

  /** Needs to be lazy so that route collecting is done after SockJS config */
  lazy val routes: RouteCollection = {
    val (normal, sockJs) = deserializeCacheFileOrRecollectWithRetry()
    sockJsClassAndOptionsTable.keys.foreach { prefix =>
      sockJs.firstGETs      .foreach { r => normal.firstGETs      .append(r.addPrefix(prefix)) }
      sockJs.firstPOSTs     .foreach { r => normal.firstPOSTs     .append(r.addPrefix(prefix)) }
      sockJs.firstPUTs      .foreach { r => normal.firstPUTs      .append(r.addPrefix(prefix)) }
      sockJs.firstDELETEs   .foreach { r => normal.firstDELETEs   .append(r.addPrefix(prefix)) }
      sockJs.firstOPTIONSs  .foreach { r => normal.firstOPTIONSs  .append(r.addPrefix(prefix)) }
      sockJs.firstWEBSOCKETs.foreach { r => normal.firstWEBSOCKETs.append(r.addPrefix(prefix)) }

      sockJs.lastGETs      .foreach { r => normal.lastGETs      .append(r.addPrefix(prefix)) }
      sockJs.lastPOSTs     .foreach { r => normal.lastPOSTs     .append(r.addPrefix(prefix)) }
      sockJs.lastPUTs      .foreach { r => normal.lastPUTs      .append(r.addPrefix(prefix)) }
      sockJs.lastDELETEs   .foreach { r => normal.lastDELETEs   .append(r.addPrefix(prefix)) }
      sockJs.lastOPTIONSs  .foreach { r => normal.lastOPTIONSs  .append(r.addPrefix(prefix)) }
      sockJs.lastWEBSOCKETs.foreach { r => normal.lastWEBSOCKETs.append(r.addPrefix(prefix)) }

      sockJs.otherGETs      .foreach { r => normal.otherGETs      .append(r.addPrefix(prefix)) }
      sockJs.otherPOSTs     .foreach { r => normal.otherPOSTs     .append(r.addPrefix(prefix)) }
      sockJs.otherPUTs      .foreach { r => normal.otherPUTs      .append(r.addPrefix(prefix)) }
      sockJs.otherDELETEs   .foreach { r => normal.otherDELETEs   .append(r.addPrefix(prefix)) }
      sockJs.otherOPTIONSs  .foreach { r => normal.otherOPTIONSs  .append(r.addPrefix(prefix)) }
      sockJs.otherWEBSOCKETs.foreach { r => normal.otherWEBSOCKETs.append(r.addPrefix(prefix)) }
    }
    normal.toRouteCollection
  }

  //----------------------------------------------------------------------------

  /** @return (normal routes, SockJS routes) */
  private def deserializeCacheFileOrRecollectWithRetry(): (SerializableRouteCollection, SerializableRouteCollection) = {
    try {
      logger.info("Load file " + ROUTES_CACHE + " or recollect routes...")
      val routeCollector = new RouteCollector
      routeCollector.deserializeCacheFileOrRecollect(ROUTES_CACHE)
    } catch {
      case NonFatal(e) =>
        Config.exitOnError("Could not collect routes", e)
        throw e
    }
  }

  //----------------------------------------------------------------------------

  private val sockJsClassAndOptionsTable = MMap[String, SockJsClassAndOptions]()

  def printSockJsRoutes() {
    // This method is only run once on start, speed is not a problem

    if (!sockJsClassAndOptionsTable.isEmpty) {
      val (pathPrefixMaxLength, handlerClassNameMaxLength, websocketOptionMaxLength) =
        sockJsClassAndOptionsTable.toList.foldLeft((0, 0, "websocket: true,".length)) {
            case ((pmax, hmax, wmax), (pathPrefix, sockJsClassAndOptions)) =>
          val plen  = pathPrefix.length
          val hlen  = sockJsClassAndOptions.handlerClass.getName.length
          val pmax2 = if (pmax < plen) plen else pmax
          val hmax2 = if (hmax < hlen) hlen else hmax
          val wmax2 = if (sockJsClassAndOptions.websocket) wmax else "websocket: false,".length
          (pmax2, hmax2, wmax2)
        }
      val logFormat = "%-" + pathPrefixMaxLength + "s  %-" + handlerClassNameMaxLength + "s  %-" + websocketOptionMaxLength + "s %s"

      val strings = sockJsClassAndOptionsTable.map { case (pathPrefix, sockJsClassAndOptions) =>
        logFormat.format(
          pathPrefix,
          sockJsClassAndOptions.handlerClass.getName,
          "websocket: " + sockJsClassAndOptions.websocket + ",",
          "cookie_needed: " + sockJsClassAndOptions.cookieNeeded
        )
      }
      logger.info("SockJS routes:\n" + strings.mkString("\n"))
    }
  }

  /**
   * Mounts SockJS handler at the path prefix.
   *
   * @param websocket set to true to enable WebSocket
   * @param cookieNeeded set to true for load balancers that needs JSESSION cookie
   */
  def sockJs(handlerClass: Class[_ <: SockJsActor], pathPrefix: String, websocket: Boolean, cookieNeeded: Boolean) {
    sockJsClassAndOptionsTable(pathPrefix) = new SockJsClassAndOptions(handlerClass, websocket, cookieNeeded)
  }

  def createSockJsActor(pathPrefix: String): ActorRef = {
    val sockJsClassAndOptions = sockJsClassAndOptionsTable(pathPrefix)
    val actorClass            = sockJsClassAndOptions.handlerClass
    Config.actorSystem.actorOf(Props {
      ConstructorAccess.get(actorClass).newInstance().asInstanceOf[Actor]
    })
  }

  /** @param sockJsHandlerClass Normal SockJsHandler subclass or object class */
  def sockJsPathPrefix(sockJsHandlerClass: Class[_ <: SockJsActor]): String = {
    val kv = sockJsClassAndOptionsTable.find { case (k, v) => v.handlerClass == sockJsHandlerClass }
    kv match {
      case Some((k, v)) => "/" + k
      case None         => throw new Exception("Cannot lookup SockJS URL for class: " + sockJsHandlerClass)
    }
  }

  def sockJsClassAndOptions(pathPrefix: String) = {
    sockJsClassAndOptionsTable(pathPrefix)
  }
}
