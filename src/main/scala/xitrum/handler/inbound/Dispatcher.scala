package xitrum.handler.inbound

import java.io.File
import java.nio.file.Paths
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
import xitrum.handler.{HandlerEnv, NoRealPipelining}
import xitrum.handler.outbound.XSendFile
import xitrum.scope.request.PathInfo
import xitrum.sockjs.SockJsPrefix
import xitrum.util.{ClassFileLoader, FileMonitor}

private class ReloadableDispatcher {
  private val CLASS_OF_ACTOR         = classOf[Actor]  // Can't be ActorAction, to support WebSocketAction and SockJsAction
  private val CLASS_OF_FUTURE_ACTION = classOf[FutureAction]

  def dispatch(actionClass: Class[_], handlerEnv: HandlerEnv) {
    // This method should be fast because it is run for every request
    // => Use ReflectASM instead of normal reflection to create action instance

    if (CLASS_OF_ACTOR.isAssignableFrom(actionClass)) {
      val actorRef = Config.actorSystem.actorOf(Props {
        val actor = Dispatcher.newActionInstance(actionClass)
        setPathPrefixForSockJs(actor, handlerEnv)
        actor.asInstanceOf[Actor]
      })
      actorRef ! handlerEnv
    } else {
      val action = Dispatcher.newActionInstance(actionClass).asInstanceOf[Action]
      setPathPrefixForSockJs(action, handlerEnv)
      action.apply(handlerEnv)
      if (CLASS_OF_FUTURE_ACTION.isAssignableFrom(actionClass)) {
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

object Dispatcher {
  private val DEVELOPMENT_MODE_CLASSES_DIR = "target/scala-2.11/classes"

  private val prodDispatcher = new ReloadableDispatcher

  def dispatch(actionClass: Class[_], handlerEnv: HandlerEnv) {
    if (Config.productionMode) {
      prodDispatcher.dispatch(actionClass, handlerEnv)
    } else {
      val classLoader = devRenewClassLoaderIfNeeded()
      devDispatch(classLoader, actionClass, handlerEnv)
    }
  }

  /** Use Class#newInstance for development mode, ConstructorAccess for production mode. */
  def newActionInstance(actionClass: Class[_]) = {
    if (Config.productionMode)
      ConstructorAccess.get(actionClass).newInstance()
    else
      actionClass.newInstance()
  }

  //----------------------------------------------------------------------------

  private var devClassLoader        = new ClassFileLoader(DEVELOPMENT_MODE_CLASSES_DIR, getClass.getClassLoader)
  private var devNeedNewClassLoader = false

  // In development mode, watch the directory "classes". If there's modification,
  // mark that at the next request, a new class loader should be created.
  if (!Config.productionMode) {
    val target = Paths.get(DEVELOPMENT_MODE_CLASSES_DIR).toAbsolutePath
    FileMonitor.monitorRecursive(FileMonitor.MODIFY, target, { path =>
      DEVELOPMENT_MODE_CLASSES_DIR.synchronized { devNeedNewClassLoader = true }
    })
  }

  private def devRenewClassLoaderIfNeeded(): ClassLoader = {
    DEVELOPMENT_MODE_CLASSES_DIR.synchronized {
      if (devNeedNewClassLoader) {
        devClassLoader        = new ClassFileLoader(DEVELOPMENT_MODE_CLASSES_DIR, getClass.getClassLoader)
        devNeedNewClassLoader = false
      }
      devClassLoader
    }
  }

  private def devDispatch(classLoader: ClassLoader, actionClass: Class[_], handlerEnv: HandlerEnv) {
    // Our main purpose
    val reloadedActionClass = classLoader.loadClass(actionClass.getName)

    // In development mode, when .class files change, we can't reuse
    // ReloadableDispatcher instance because of the way it's written.
    // Instead, we must also reload it using a new class loader.
    val reloadableDispatcherClass    = classLoader.loadClass(classOf[ReloadableDispatcher].getName)
    val reloadableDispatcherInstance = reloadableDispatcherClass.newInstance()
    val reloadableDispatcherMethod   = reloadableDispatcherClass.getMethod("dispatch", classOf[Class[_]], classOf[HandlerEnv])
    reloadableDispatcherMethod.invoke(reloadableDispatcherInstance, reloadedActionClass, handlerEnv)
  }
}

//------------------------------------------------------------------------------

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
        XSendFile.set404Page(response, false)
        ctx.channel.writeAndFlush(env)

      case Some(error404) =>
        env.pathParams = MMap.empty
        env.response.setStatus(NOT_FOUND)
        Dispatcher.dispatch(error404, env)
    }
  }
}
