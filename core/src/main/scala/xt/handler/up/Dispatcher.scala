package xt.handler.up

import xt._
import xt.handler._
import xt.vc.Controller

import java.lang.reflect.Method

import org.jboss.netty.channel._
import org.jboss.netty.handler.codec.http.{HttpRequest, HttpResponse, HttpMethod, HttpResponseStatus}

class Dispatcher extends RequestHandler {
  def handleRequest(ctx: ChannelHandlerContext, env: XtEnv) {
    //import env._

        new App {
      def call(channel: Channel, request: HttpRequest, response: HttpResponse, env: Env) {
        val (ka, uriParams) = matchRoute(env.method, env.pathInfo) match {
          case Some((ka, uriParams)) =>
            (ka, uriParams)

          case None =>
            response.setStatus(HttpResponseStatus.NOT_FOUND)
            val uriParams = new java.util.LinkedHashMap[String, java.util.List[String]]()
            uriParams.put("controller", toValues(csast404._1))
            uriParams.put("action",     toValues(csast404._2))
            (ka404, uriParams)
        }

        // Put (csast500, ka500) to env so that they can be taken out later
        // by Failsafe midddleware
        env.put("error500", compiledCsas500)

        logger.debug(env.method + " " + env.pathInfo)  // TODO: Fix this ugly code (1 of 3)
        dispatch(app, channel, request, response, env, ka, uriParams)
      }
    }

  }

  /**
   * WARN: This method is here because it is also used by Failsafe when redispatching.
   */
  def dispatch(app: App,
               channel: Channel, request: HttpRequest, response: HttpResponse, env: Env,
               ka: KA, uriParams: UriParams) {
    // Merge uriParams to params
    env.params.putAll(uriParams)

    // Put controller (Controller) and action (Method) to env so that
    // the action can be invoked at XTApp
    val (k, a) = ka
    val c = k.newInstance
    env.controller = c
    env.action     = a

    logger.debug(filterParams(env.params).toString)  // TODO: Fix this ugly code (2 of 3)
    val t1 = System.currentTimeMillis
    app.call(channel, request, response, env)
    val t2 = System.currentTimeMillis
    logger.debug((t2 - t1) + " [ms]")                // TODO: Fix this ugly code (3 of 3)
  }
}
