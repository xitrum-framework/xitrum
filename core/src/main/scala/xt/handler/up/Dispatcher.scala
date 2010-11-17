package xt.handler.up

import xt._
import xt.vc._
import xt.vc.controller.MissingParam

import java.lang.reflect.{Method, InvocationTargetException}

import org.jboss.netty.channel._
import org.jboss.netty.buffer.ChannelBuffers
import org.jboss.netty.util.CharsetUtil
import org.jboss.netty.handler.codec.http._
import HttpResponseStatus._
import HttpVersion._

class Dispatcher extends SimpleChannelUpstreamHandler with Logger {
  override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {
    val m = e.getMessage
    if (!m.isInstanceOf[BodyParserResult]) {
      ctx.sendUpstream(e)
      return
    }

    val bpr = m.asInstanceOf[BodyParserResult]
    val request = bpr.request
    val pathInfo = bpr.pathInfo
    val uriParams = bpr.uriParams
    val bodyParams = bpr.bodyParams

    Router.matchRoute(request.getMethod, pathInfo) match {
      case Some((ka, pathParams)) =>
        dispatchWithFailsafe(ctx, bpr, ka, pathParams)

      case None =>
        val response = new DefaultHttpResponse(HTTP_1_1, OK)
        response.setStatus(NOT_FOUND)
        response.setHeader("X-Sendfile", System.getProperty("user.dir") + "/public/404.html")
        ctx.getChannel.write(response)
    }
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, e: ExceptionEvent) {
    logger.error("Dispatcher", e.getCause)
    e.getChannel.close
  }

  //----------------------------------------------------------------------------

  private def dispatchWithFailsafe(ctx: ChannelHandlerContext, bpr: BodyParserResult, ka: Router.KA, pathParams: Env.Params) {
    try {
      val (klass, action) = ka
      val controller      = klass.newInstance
      controller(ctx, bpr.request, bpr.pathInfo, bpr.uriParams, bpr.bodyParams, pathParams)

      logger.debug(bpr.request.getMethod + " " + bpr.pathInfo)
      logger.debug(filterParams(controller.allParams).toString)

      // Begin timestamp
      val t1 = System.currentTimeMillis

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

      // End timestamp
      val t2 = System.currentTimeMillis
      logger.debug((t2 - t1) + " [ms]")
    } catch {
      case e =>
        val response = new DefaultHttpResponse(HTTP_1_1, OK)

        // MissingParam is a special case

        if (e.isInstanceOf[InvocationTargetException]) {
          val ite = e.asInstanceOf[InvocationTargetException]
          val c = ite.getCause
          if (c.isInstanceOf[MissingParam]) {
            val mp = c.asInstanceOf[MissingParam]
            val key = mp.key
            response.setStatus(BAD_REQUEST)
            val cb = ChannelBuffers.copiedBuffer("Missing Param: " + key, CharsetUtil.UTF_8)
            HttpHeaders.setContentLength(response, cb.readableBytes)
            response.setContent(cb)
          }
        }

        if (response.getStatus != BAD_REQUEST) {
          logger.error("Error on dispatching", e)
          response.setStatus(INTERNAL_SERVER_ERROR)
          response.setHeader("X-Sendfile", System.getProperty("user.dir") + "/public/500.html")
        }

        ctx.getChannel.write(response)
    }
  }

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
