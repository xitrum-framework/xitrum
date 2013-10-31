package xitrum.handler.up

import java.io.File
import scala.collection.mutable.{Map => MMap}

import org.jboss.netty.channel._
import org.jboss.netty.handler.codec.http._
import ChannelHandler.Sharable
import HttpResponseStatus._
import HttpVersion._

import akka.actor.{Actor, Props}
import com.esotericsoftware.reflectasm.ConstructorAccess

import xitrum.{Action, ActionActor, Config}
import xitrum.etag.NotModified
import xitrum.handler.{AccessLog, HandlerEnv}
import xitrum.handler.down.XSendFile
import xitrum.scope.request.PathInfo
import xitrum.sockjs.SockJsPrefix

object Dispatcher {
  private val classOfActor = classOf[Actor]

  def dispatch(klass: Class[_], handlerEnv: HandlerEnv) {
    // This method should be fast because it is run for every request
    // => Use ReflectASM instead of normal reflection to create action instance

    if (classOfActor.isAssignableFrom(klass)) {
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
      instance.asInstanceOf[SockJsPrefix].setPathPrefix(handlerEnv.pathInfo)
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

    val env      = m.asInstanceOf[HandlerEnv]
    val request  = env.request
    val pathInfo = env.pathInfo

    if (handleOPTIONS(ctx, env, request, pathInfo)) return

    // Look up GET if method is HEAD
    val requestMethod = if (request.getMethod == HttpMethod.HEAD) HttpMethod.GET else request.getMethod
    Config.routes.route(requestMethod, pathInfo) match {
      case Some((route, pathParams)) =>
        env.route      = route
        env.pathParams = pathParams
        Dispatcher.dispatch(route.klass, env)

      case None =>
        if (!handleIndexHtmlFallback(ctx, env, pathInfo)) handle404(ctx, env)
    }
  }

  /** @return true if the request has been handled */
  private def handleOPTIONS(ctx: ChannelHandlerContext, env: HandlerEnv, request: HttpRequest, pathInfo: PathInfo): Boolean = {
    if (request.getMethod == HttpMethod.OPTIONS) {
      Config.routes.route(HttpMethod.GET,    pathInfo) orElse
      Config.routes.route(HttpMethod.POST,   pathInfo) orElse
      Config.routes.route(HttpMethod.PUT,    pathInfo) orElse
      Config.routes.route(HttpMethod.PATCH,  pathInfo) orElse
      Config.routes.route(HttpMethod.DELETE, pathInfo) match {
        case Some((route, pathParams)) =>
          env.route = route
          env.response = new DefaultHttpResponse(HTTP_1_1, NO_CONTENT)
          ctx.getChannel.write(env)
          AccessLog.logOPTIONS(request)

        case None =>
          handle404(ctx, env)
      }
      true
    } else {
      false
    }
  }

  /** @return true if the request has been handled */
  private def handleIndexHtmlFallback(ctx: ChannelHandlerContext, env: HandlerEnv, pathInfo: PathInfo):Boolean = {
    // Try to fallback to index.html if it exists
    val staticPath = Config.root + "/public" + pathInfo.decodedWithIndexHtml
    val file       = new File(staticPath)
    if (file.isFile && file.exists) {
      val response = new DefaultHttpResponse(HTTP_1_1, OK)

      if (!Config.xitrum.staticFile.revalidate)
        NotModified.setClientCacheAggressively(response)

      XSendFile.setHeader(response, staticPath, false)
      env.response = response
      ctx.getChannel.write(env)
      true
    } else {
      false
    }
  }

  private def handle404(ctx: ChannelHandlerContext, env: HandlerEnv) {
    Config.routes.error404 match {
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
