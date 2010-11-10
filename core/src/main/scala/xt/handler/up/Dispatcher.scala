package xt.handler.up

import xt._
import xt.vc._
import xt.vc.helper._

import java.lang.reflect.Method

import org.jboss.netty.channel._
import org.jboss.netty.handler.codec.http._
import HttpResponseStatus._
import HttpVersion._

class Dispatcher extends RequestHandler {
  def handleRequest(ctx: ChannelHandlerContext, env: Env) {
    import env._

    lastUpstreamHandlerCtx = ctx

    Router.matchRoute(env.method, env.pathInfo) match {
      case Some((ka, uriParams)) =>
        logger.debug(env.method + " " + env.pathInfo)
        dispatch(env, ka, uriParams)

      case None =>
        response.setStatus(NOT_FOUND)
        env.response.setHeader("X-Sendfile", System.getProperty("user.dir") + "/public/404.html")
        env.lastUpstreamHandlerCtx.getChannel.write(env)
    }
  }

  /**
   * WARN: This method is here because it is also used by Failsafe when redispatching.
   */
  def dispatch(env: Env, ka: Router.KA, uriParams: Router.UriParams) {
    // Merge uriParams to params
    env.allParams.putAll(uriParams)
    logger.debug(filterParams(env.allParams).toString)

    // Put controller (Controller) and action (Method) to env so that
    // the action can be invoked at XTApp
    val (k, action) = ka
    val controller = k.newInstance
    controller(env)

    // Begin timestamp
    val t1 = System.currentTimeMillis

    // Failsafe
    try {
      // Call before filters
      val passed = controller.beforeFilters.forall(filter => {
        val onlyActions = filter._2
        if (onlyActions.isEmpty) {
          val exceptActions = filter._3
          if (!exceptActions.contains(action)) {
            val method = filter._1
            method.invoke(controller).asInstanceOf[Boolean]
          }	else true
        } else {
          if (onlyActions.contains(action)) {
            val method = filter._1
            method.invoke(controller).asInstanceOf[Boolean]
          } else true
        }
      })

      // Call action
      if (passed) action.invoke(controller)
    } catch {
      case e =>
        logger.error("Error on dispatching", e)

        env.response.setStatus(INTERNAL_SERVER_ERROR)
        env.response.setHeader("X-Sendfile", System.getProperty("user.dir") + "/public/500.html")
        env.lastUpstreamHandlerCtx.getChannel.write(env)
    }

    // End timestamp
    val t2 = System.currentTimeMillis
    logger.debug((t2 - t1) + " [ms]")
  }

  //----------------------------------------------------------------------------

  // Same as Rails' config.filter_parameters
  private def filterParams(params: java.util.Map[String, java.util.List[String]]): java.util.Map[String, java.util.List[String]] = {
    val ret = new java.util.LinkedHashMap[String, java.util.List[String]]()
    ret.putAll(params)
    for (key <- Config.filterParams) {
      if (ret.containsKey(key)) ret.put(key, Router.toValues("[filtered]"))
    }
    ret
  }
}
