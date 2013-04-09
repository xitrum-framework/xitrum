package xitrum.handler.up

import scala.collection.mutable.{ArrayBuffer, Map => MMap}

import org.jboss.netty.channel._
import org.jboss.netty.handler.codec.http._
import ChannelHandler.Sharable
import HttpResponseStatus._
import HttpVersion._

import akka.actor.{Actor, ActorRef, Props}
import com.esotericsoftware.reflectasm.ConstructorAccess

import xitrum.{Action, Config}
import xitrum.handler.HandlerEnv
import xitrum.handler.down.XSendFile
import xitrum.routing.Routes

object Dispatcher {
  def dispatchWithFailsafe(actionClass: Class[_ <: Action], cacheSecs: Int, env: HandlerEnv) {
    val system   = Config.actorSystem
    val actorRef = system.actorOf(Props(ConstructorAccess.get(actionClass).newInstance().asInstanceOf[Actor]))
    actorRef ! (env, cacheSecs)
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
        Dispatcher.dispatchWithFailsafe(route.actionClass, route.cacheSecs, env)

      case None =>
        if (Routes.error404 == null) {
            val response = new DefaultHttpResponse(HTTP_1_1, NOT_FOUND)
            XSendFile.set404Page(response, false)
            env.response = response
            ctx.getChannel.write(env)
        } else {
            env.pathParams = MMap.empty
            env.response.setStatus(NOT_FOUND)
            Dispatcher.dispatchWithFailsafe(Routes.error404, 0, env)
        }
    }
  }
}
