package xitrum.routing

import scala.collection.mutable.{Map => MMap}

import akka.actor.{Actor, ActorRef, ActorRefFactory, Props}
import com.esotericsoftware.reflectasm.ConstructorAccess

import xitrum.{Config, Log, SockJsAction}

class SockJsRouteMap(map: Map[String, Class[_ <: SockJsAction]]) extends Log {
  def logRoutes() {
    // This method is only run once on start, speed is not a problem

    if (!map.isEmpty) {
      val (pathPrefixMaxLength, handlerClassNameMaxLength) =
        map.toList.foldLeft((0, 0)) {
            case ((pmax, hmax), (pathPrefix, sockJsClass)) =>
          val plen  = pathPrefix.length
          val hlen  = sockJsClass.getName.length
          val pmax2 = if (pmax < plen) plen else pmax
          val hmax2 = if (hmax < hlen) hlen else hmax
          (pmax2, hmax2)
        }
      val logFormat = "%-" + pathPrefixMaxLength + "s  %-" + handlerClassNameMaxLength + "s"

      val strings = map.map { case (pathPrefix, sockJsClass) =>
        logFormat.format(pathPrefix, sockJsClass.getName)
      }
      log.info("SockJS routes:\n" + strings.mkString("\n"))
    }
  }

  /** Creates actor attached to Config.actorSystem. */
  def createSockJsAction(pathPrefix: String): ActorRef = {
    createSockJsAction(Config.actorSystem, pathPrefix)
  }

  /** Creates actor attached to the given context. */
  def createSockJsAction(context: ActorRefFactory, pathPrefix: String): ActorRef = {
    val sockJsActorClass = map(pathPrefix)
    context.actorOf(Props(ConstructorAccess.get(sockJsActorClass).newInstance()))
  }

  /** @param sockJsHandlerClass Normal SockJsHandler subclass or object class */
  def findPathPrefix(sockJsActorClass: Class[_ <: SockJsAction]): String = {
    val kv = map.find { case (k, v) => v == sockJsActorClass }
    kv match {
      case Some((k, v)) => "/" + k
      case None         => throw new Exception("Cannot lookup SockJS URL for class: " + sockJsActorClass)
    }
  }
}
