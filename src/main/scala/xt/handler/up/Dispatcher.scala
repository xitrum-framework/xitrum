package xt.handler.up

import java.lang.reflect.{Method, InvocationTargetException}
import java.util.{Map => JMap, List => JList, LinkedHashMap}

import org.jboss.netty.channel._
import org.jboss.netty.buffer.ChannelBuffers
import org.jboss.netty.util.CharsetUtil
import org.jboss.netty.handler.codec.http._
import ChannelHandler.Sharable
import HttpResponseStatus._
import HttpVersion._

import xt.{Config, Action, MissingParam, Postback}
import xt.handler.Env
import xt.routing.{Routes, Util}
import xt.vc.env.PathInfo
import xt.vc.env.{Env => CEnv}

@Sharable
class Dispatcher extends SimpleChannelUpstreamHandler with ClosedClientSilencer {
  override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {
    val m = e.getMessage
    if (!m.isInstanceOf[Env]) {
      ctx.sendUpstream(e)
      return
    }

    val env        = m.asInstanceOf[Env]
    val request    = env("request").asInstanceOf[HttpRequest]
    val pathInfo   = env("pathInfo").asInstanceOf[PathInfo]
    val uriParams  = env("uriParams").asInstanceOf[CEnv.Params]
    val bodyParams = env("bodyParams").asInstanceOf[CEnv.Params]

    Routes.matchRoute(request.getMethod, pathInfo) match {
      case Some((actionClass, pathParams)) =>
        env("pathParams") = pathParams
        dispatchWithFailsafe(ctx, actionClass, env)

      case None =>
        val response = new DefaultHttpResponse(HTTP_1_1, NOT_FOUND)
        response.setHeader("X-Sendfile", System.getProperty("user.dir") + "/public/404.html")
        env("response") = response
        ctx.getChannel.write(env)
    }
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, e: ExceptionEvent) {
    logger.error("Dispatcher", e.getCause)
    e.getChannel.close
  }

  //----------------------------------------------------------------------------

  private def dispatchWithFailsafe(ctx: ChannelHandlerContext, actionClass: Class[Action], env: Env) {
    // Begin timestamp
    val beginTimestamp = System.currentTimeMillis

    val action = actionClass.newInstance
    action(ctx, env)

    try {
      val passed = action.callBeforeFilters
      if (passed) {
        if (action.request.getMethod.getName == "POSTBACK") {
          if (action.isInstanceOf[Postback]) {
            val csrfToken = action.param("_csrf_token")
            if (csrfToken != action.csrfToken) {
              throw new MissingParam("_csrf_token")
            } else {
              action.asInstanceOf[Postback].postback
            }
          } else {
            throw new Exception(actionClass.getName + " is not a postback class")
          }
        } else {
          action.execute
        }
      }

      logAccess(beginTimestamp, action)
    } catch {
      case e =>
        // End timestamp
        val t2 = System.currentTimeMillis

        val response = new DefaultHttpResponse(HTTP_1_1, INTERNAL_SERVER_ERROR)

        // MissingParam is a special case

        if (e.isInstanceOf[InvocationTargetException]) {
          val ite = e.asInstanceOf[InvocationTargetException]
          val c = ite.getCause
          if (c.isInstanceOf[MissingParam]) {
            response.setStatus(BAD_REQUEST)
            val mp  = c.asInstanceOf[MissingParam]
            val key = mp.key
            val cb  = ChannelBuffers.copiedBuffer("Missing Param: " + key, CharsetUtil.UTF_8)
            HttpHeaders.setContentLength(response, cb.readableBytes)
            response.setContent(cb)
          }
        }

        if (response.getStatus != BAD_REQUEST) {
          logAccess(beginTimestamp, action, e)
          response.setHeader("X-Sendfile", System.getProperty("user.dir") + "/public/500.html")
        } else {
          logAccess(beginTimestamp, action)
        }

        env("response") = response
        ctx.getChannel.write(env)
    }
  }

  private def logAccess(beginTimestamp: Long, action: Action, e: Throwable = null) {
    val endTimestamp = System.currentTimeMillis
    val dt           = endTimestamp - beginTimestamp

    val async = if (action.responded) "" else " (async)"
    if (e == null) {
      val msg =
        action.request.getMethod       + " " +
        action.pathInfo.decoded        + " " +
        filterParams(action.allParams) + " " +
        dt + " [ms]" + async
      logger.debug(msg)
    } else {
      val msg =
        "Dispatching error " +
        action.request.getMethod       + " " +
        action.pathInfo.decoded        + " " +
        filterParams(action.allParams) + " " +
        dt + " [ms]" + async
      logger.error(msg, e)
    }
  }

  // Same as Rails' config.filter_parameters
  private def filterParams(params: java.util.Map[String, java.util.List[String]]): java.util.Map[String, java.util.List[String]] = {
    val ret = new java.util.LinkedHashMap[String, java.util.List[String]]()
    ret.putAll(params)
    for (key <- Config.filterParams) {
      if (ret.containsKey(key)) ret.put(key, Util.toValues("*FILTERED*"))
    }
    ret
  }
}
