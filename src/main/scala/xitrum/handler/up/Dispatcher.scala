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
import xitrum.sockjs.SockJsPrefix

object Dispatcher {
  def dispatch(klass: Class[_], handlerEnv: HandlerEnv) {
    if (classOf[Actor].isAssignableFrom(klass)) {
      val actorRef = Config.actorSystem.actorOf(Props {
        val actor = ConstructorAccess.get(klass).newInstance()
        setPathPrefixForSockJs(actor, handlerEnv)
        actor.asInstanceOf[Actor]
      })
      actorRef ! handlerEnv
    } else {
      val action = ConstructorAccess.get(klass).newInstance().asInstanceOf[Action]
      setPathPrefixForSockJs(action, handlerEnv)
      action.apply(handlerEnv)
      action.dispatchWithFailsafe()
    }
  }

  private def setPathPrefixForSockJs(instance: Any, handlerEnv: HandlerEnv) {
    if (instance.isInstanceOf[SockJsPrefix])
      instance.asInstanceOf[SockJsPrefix].pathPrefix = handlerEnv.pathInfo.tokens(0)
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
        Dispatcher.dispatch(route.klass, env)

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
