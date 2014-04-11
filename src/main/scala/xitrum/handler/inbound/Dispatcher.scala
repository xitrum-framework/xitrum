package xitrum.handler.inbound

import java.io.File
import scala.collection.mutable.{Map => MMap}
import io.netty.channel._
import io.netty.handler.codec.http._
import ChannelHandler.Sharable
import HttpResponseStatus._
import HttpVersion._

import akka.actor.{Actor, Props}
import com.esotericsoftware.reflectasm.ConstructorAccess

import xitrum.{Action, ActorAction, FutureAction, Config}
import xitrum.etag.NotModified
import xitrum.handler.{AccessLog, HandlerEnv, NoRealPipelining}
import xitrum.handler.outbound.XSendFile
import xitrum.scope.request.PathInfo
import xitrum.sockjs.SockJsPrefix

object Dispatcher {
  private val CLASS_OF_ACTOR         = classOf[Actor]  // Can't be ActorAction, to support WebSocketAction and SockJsAction
  private val CLASS_OF_FUTURE_ACTION = classOf[FutureAction]

  def dispatch(klass: Class[_], handlerEnv: HandlerEnv) {
    // This method should be fast because it is run for every request
    // => Use ReflectASM instead of normal reflection to create action instance

    if (CLASS_OF_ACTOR.isAssignableFrom(klass)) {
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
      if (CLASS_OF_FUTURE_ACTION.isAssignableFrom(klass)) {
        Config.actorSystem.dispatcher.execute(new Runnable {
          def run() { action.dispatchWithFailsafe() }
        })
      } else {
        action.dispatchWithFailsafe()
      }
    }
  }

  private def setPathPrefixForSockJs(instance: Any, handlerEnv: HandlerEnv) {
    if (instance.isInstanceOf[SockJsPrefix])
      instance.asInstanceOf[SockJsPrefix].setPathPrefix(handlerEnv.pathInfo)
  }
}

@Sharable
class Dispatcher extends SimpleChannelInboundHandler[HandlerEnv] {
  override def channelRead0(ctx: ChannelHandlerContext, env: HandlerEnv) {
    val request  = env.request
    val pathInfo = env.pathInfo

    if (request.getMethod == HttpMethod.OPTIONS) {
      val future = ctx.channel.writeAndFlush(env)
      NoRealPipelining.if_keepAliveRequest_then_resumeReading_else_closeOnComplete(request, env.channel, future)
      return
    }

    // Look up GET if method is HEAD
    val requestMethod = if (request.getMethod == HttpMethod.HEAD) HttpMethod.GET else request.getMethod
    Config.routes.route(requestMethod, pathInfo) match {
      case Some((route, pathParams)) =>
        env.route      = route
        env.pathParams = pathParams
        env.response.setStatus(OK)
        Dispatcher.dispatch(route.klass, env)

      case None =>
        if (!handleIndexHtmlFallback(ctx, env, pathInfo)) handle404(ctx, env)
    }
  }

  /** @return true if the request has been handled */
  private def handleIndexHtmlFallback(ctx: ChannelHandlerContext, env: HandlerEnv, pathInfo: PathInfo): Boolean = {
    // Try to fallback to index.html if it exists
    val staticPath = Config.root + "/public" + pathInfo.decodedWithIndexHtml
    val file       = new File(staticPath)
    if (file.isFile && file.exists) {
      val response = env.response
      if (!Config.xitrum.staticFile.revalidate)
        NotModified.setClientCacheAggressively(response)

      XSendFile.setHeader(response, staticPath, false)
      ctx.channel.writeAndFlush(env)
      true
    } else {
      false
    }
  }

  private def handle404(ctx: ChannelHandlerContext, env: HandlerEnv) {
    Config.routes.error404 match {
      case None =>
        val response = env.response
        response.setStatus(NOT_FOUND)
        XSendFile.set404Page(response, false)
        ctx.channel.writeAndFlush(env)

      case Some(error404) =>
        env.pathParams = MMap.empty
        env.response.setStatus(NOT_FOUND)
        Dispatcher.dispatch(error404, env)
    }
  }
}
