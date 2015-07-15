package xitrum.routing

import scala.collection.mutable.{Map => MMap}
import akka.actor.{Actor, ActorRef, ActorRefFactory, Props}
import xitrum.{Config, Log, SockJsAction}
import xitrum.handler.inbound.Dispatcher

/**
 * "websocket" and "cookieNeeded" members are named after SockJS options, ex:
 * {"websocket": true, "cookie_needed": false, "origins": ["*:*"], "entropy": 123}
 *
 * - websocket: true means WebSocket is enabled
 * - cookieNeeded: true means load balancers needs JSESSION cookie
 */
class SockJsClassAndOptions(
  val actionClass:  Class[_ <: SockJsAction],  // Classes that extend SockJsAction are not always actor
  val websocket:    Boolean,
  val cookieNeeded: Boolean
) extends Serializable

class SockJsRouteMap(map: MMap[String, SockJsClassAndOptions]) {
  /** @param xitrumRoutes true: log only Xitrum routes, false: log only app routes */
  def logRoutes(xitrumRoutes: Boolean) {
    // This method is only run once on start, speed is not a problem

    val map = this.map.filter { case (path, sockJsClassAndOptions) =>
      sockJsClassAndOptions.actionClass.getName.startsWith("xitrum") == xitrumRoutes
    }

    if (map.isEmpty) return

    val (pathPrefixMaxLength, handlerClassNameMaxLength, websocketOptionMaxLength) =
      map.toList.foldLeft((0, 0, "websocket: true,".length)) {
          case ((pmax, hmax, wmax), (pathPrefix, sockJsClassAndOptions)) =>
        val plen  = pathPrefix.length
        val hlen  = sockJsClassAndOptions.actionClass.getName.length
        val pmax2 = if (pmax < plen) plen else pmax
        val hmax2 = if (hmax < hlen) hlen else hmax
        val wmax2 = if (sockJsClassAndOptions.websocket) wmax else "websocket: false,".length
        (pmax2, hmax2, wmax2)
      }
    val logFormat = "%-" + (pathPrefixMaxLength + 1) + "s  %-" + handlerClassNameMaxLength + "s  %-" + websocketOptionMaxLength + "s %s"

    val strings = map.map { case (pathPrefix, sockJsClassAndOptions) =>
      logFormat.format(
        "/" + pathPrefix,
        sockJsClassAndOptions.actionClass.getName,
        "websocket: " + sockJsClassAndOptions.websocket + ",",
        "cookie_needed: " + sockJsClassAndOptions.cookieNeeded
      )
    }
    if (xitrumRoutes)
      Log.info("Xitrum SockJS routes:\n" + strings.mkString("\n"))
    else
      Log.info("SockJS routes:\n" + strings.mkString("\n"))
  }

  /** Creates actor attached to Config.actorSystem. */
  def createSockJsAction(pathPrefix: String): ActorRef = {
    createSockJsAction(Config.actorSystem, pathPrefix)
  }

  /** Creates actor attached to the given context. */
  def createSockJsAction(context: ActorRefFactory, pathPrefix: String): ActorRef = {
    val sockJsClassAndOptions = map(pathPrefix)
    val actorClass            = sockJsClassAndOptions.actionClass
    context.actorOf(Props(Dispatcher.newAction(actorClass).asInstanceOf[Actor]))
  }

  /** @param sockJsActorClass Normal SockJsHandler subclass or object class */
  def findPathPrefix(sockJsActorClass: Class[_ <: SockJsAction]): String = {
    // Need to compare by class name because the classes may be loaded by
    // different class loaders
    val className = sockJsActorClass.getName
    val kv = map.find { case (k, v) => v.actionClass.getName == className }
    kv match {
      case Some((k, v)) => "/" + k
      case None         => throw new Exception("Cannot lookup SockJS URL for class: " + sockJsActorClass)
    }
  }

  def lookup(pathPrefix: String) = map(pathPrefix)

  def removeByPrefix(withoutSlashPrefix: String) = {
    map.map { case (pathPrefixOfAction, sockJsClassAndOptions) =>
      if (pathPrefixOfAction.startsWith(withoutSlashPrefix)) map.remove(pathPrefixOfAction)
    }
  }
}
