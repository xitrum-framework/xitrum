package xitrum.handler.up

import scala.collection.mutable.{Map => MMap}

import org.jboss.netty.channel._
import org.jboss.netty.handler.codec.http._
import ChannelHandler.Sharable
import HttpResponseStatus._
import HttpVersion._

import akka.actor.{Actor, Props}
import com.esotericsoftware.reflectasm.ConstructorAccess

import xitrum.{Action, ActionActor, Config}
import xitrum.handler.HandlerEnv
import xitrum.handler.down.XSendFile
import xitrum.routing.Routes
import xitrum.sockjs.SockJsAction

object Dispatcher {
  def dispatch(actionClass: Class[_ <: Action], handlerEnv: HandlerEnv) {
    if (classOf[Actor].isAssignableFrom(actionClass)) {
      val actorRef = Config.actorSystem.actorOf(Props {
        val action = ConstructorAccess.get(actionClass).newInstance()
        setPathPrefixForSockJsAction(action, handlerEnv)
        action.asInstanceOf[Actor]
      })
      actorRef ! handlerEnv
    } else {
      val action = ConstructorAccess.get(actionClass).newInstance()
      setPathPrefixForSockJsAction(action, handlerEnv)
      action.apply(handlerEnv)
      action.dispatchWithFailsafe()
    }
  }

  private def setPathPrefixForSockJsAction(action: Action, handlerEnv: HandlerEnv) {
    if (action.isInstanceOf[SockJsAction])
      action.asInstanceOf[SockJsAction].pathPrefix = handlerEnv.pathInfo.tokens(0)
  }
}

@Sharable
class Dispatcher extends SimpleChannelUpstreamHandler with BadClientSilencer {
  override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {
    val m = e.getMessage
    if (!m.isInstanceOf[HandlerEnv]) {
      ctx.sendUpstream(e)
      return
    }

    val env        = m.asInstanceOf[HandlerEnv]
    val request    = env.request
    val pathInfo   = env.pathInfo
    val uriParams  = env.uriParams
    val bodyParams = env.bodyParams

    // Look up GET if method is HEAD
    val requestMethod = if (request.getMethod == HttpMethod.HEAD) HttpMethod.GET else request.getMethod
    Routes.routes.route(requestMethod, pathInfo) match {
      case Some((route, pathParams)) =>
        env.route      = route
        env.pathParams = pathParams
        Dispatcher.dispatch(route.actionClass, env)

      case None =>
        Routes.routes.error404 match {
          case None =>
            val response = new DefaultHttpResponse(HTTP_1_1, NOT_FOUND)
            XSendFile.set404Page(response, false)
            env.response = response
            ctx.getChannel.write(env)

          case Some(error404) =>
            env.pathParams = MMap.empty
            env.response.setStatus(NOT_FOUND)
            Dispatcher.dispatch(error404, env)
        }
    }
  }
}
