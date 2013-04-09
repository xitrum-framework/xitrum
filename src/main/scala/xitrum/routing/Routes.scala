package xitrum.routing

import java.io.File

import scala.collection.mutable.{Map => MMap}
import scala.util.control.NonFatal

import org.apache.commons.lang3.ClassUtils

import xitrum.{Config, Action, Logger, SockJsHandler}
import xitrum.sockjs.SockJsAction

// "websocket" and "cookieNeeded" members are named after SockJS option:
// {"websocket": true/false, "cookie_needed": true/false, "origins": ["*:*"], "entropy": integer}
class SockJsClassAndOptions(val handlerClass: Class[_ <: SockJsHandler], val websocket: Boolean, val cookieNeeded: Boolean)

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

  /** 404.html and 500.html are used by default */
  var error404: Class[_ <: Action] = _
  var error500: Class[_ <: Action] = _

  //----------------------------------------------------------------------------

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

  /**
   * Mounts SockJS handler at the path prefix.
   *
   * @param websocket set to true to enable WebSocket
   * @param cookieNeeded set to true for load balancers that needs JSESSION cookie
   */
  def sockJs(handlerClass: Class[_ <: SockJsHandler], pathPrefix: String, websocket: Boolean = true, cookieNeeded: Boolean = false) {
    sockJsClassAndOptionsTable(pathPrefix) = new SockJsClassAndOptions(handlerClass, websocket, cookieNeeded)
  }

  def createSockJsHandler(pathPrefix: String) = {
    val sockJsClassAndOptions = sockJsClassAndOptionsTable(pathPrefix)
    sockJsClassAndOptions.handlerClass.newInstance()
  }

  /** @param sockJsHandlerClass Normal SockJsHandler subclass or object class */
  def sockJsPathPrefix(sockJsHandlerClass: Class[_ <: SockJsHandler]) = {
    val className = sockJsHandlerClass.getName
    if (className.endsWith("$")) {
      val normalClassName = className.substring(0, className.length - 1)
      val normalClass     = ClassUtils.getClass(normalClassName)
      sockJsPathPrefixForNormalSockJsHandlerClass(normalClass.asInstanceOf[Class[_ <: SockJsHandler]])
    } else {
      sockJsPathPrefixForNormalSockJsHandlerClass(sockJsHandlerClass)
    }
  }

  def sockJsClassAndOptions(pathPrefix: String) = {
    sockJsClassAndOptionsTable(pathPrefix)
  }

  private def sockJsPathPrefixForNormalSockJsHandlerClass(handlerClass: Class[_ <: SockJsHandler]): String = {
    val kv = sockJsClassAndOptionsTable.find { case (k, v) => v.handlerClass == handlerClass }
    kv match {
      case Some((k, v)) => "/" + k
      case None         => throw new Exception("Cannot lookup SockJS URL for class: " + handlerClass)
    }
  }
}
