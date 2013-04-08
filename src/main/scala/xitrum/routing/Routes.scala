package xitrum.routing

import java.io.File

import scala.collection.mutable.{Map => MMap}
import scala.util.control.NonFatal

import org.apache.commons.lang3.ClassUtils

import xitrum.{Config, ActionEnv, Logger, SockJsHandler}
import xitrum.sockjs.SockJsAction

// "websocket" and "cookieNeeded" members are named after SockJS option:
// {"websocket": true/false, "cookie_needed": true/false, "origins": ["*:*"], "entropy": integer}
class SockJsClassAndOptions(val handlerClass: Class[_ <: SockJsHandler], val websocket: Boolean, val cookieNeeded: Boolean)

object Routes extends Logger {
  private val ROUTES_CACHE = "routes.cache"

  val routes = deserializeCacheFileOrRecollectWithRetry()

  /** 404.html and 500.html are used by default */
  var error404: Class[_ <: ActionEnv] = _
  var error500: Class[_ <: ActionEnv] = _

  //----------------------------------------------------------------------------

  private def deserializeCacheFileOrRecollectWithRetry(): RouteCollection = {
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
