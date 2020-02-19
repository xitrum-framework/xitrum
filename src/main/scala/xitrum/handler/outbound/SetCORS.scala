package xitrum.handler.outbound

import io.netty.channel.{ChannelHandler, ChannelHandlerContext, ChannelOutboundHandlerAdapter, ChannelPromise}
import io.netty.handler.codec.http.{HttpHeaderNames, HttpMethod, HttpResponseStatus}
import ChannelHandler.Sharable
import HttpHeaderNames._
import HttpMethod._
import HttpResponseStatus._

import xitrum.Config
import xitrum.handler.HandlerEnv

@Sharable
class SetCORS extends ChannelOutboundHandlerAdapter {
  override def write(ctx: ChannelHandlerContext, msg: Object, promise: ChannelPromise): Unit = {
    import Config.xitrum.response.corsAllowOrigins

    if (!msg.isInstanceOf[HandlerEnv] || corsAllowOrigins.isEmpty) {
      ctx.write(msg, promise)
      return
    }

    val env      = msg.asInstanceOf[HandlerEnv]
    val request  = env.request
    val response = env.response

    // Access-Control-Allow-Origin
    if (!response.headers.contains(ACCESS_CONTROL_ALLOW_ORIGIN)) {
      val requestOrigin = request.headers.get(ORIGIN)
      if (corsAllowOrigins.head.equals("*")) {
        if (requestOrigin == null || requestOrigin == "null")
          response.headers.set(ACCESS_CONTROL_ALLOW_ORIGIN, "*")
        else
          response.headers.set(ACCESS_CONTROL_ALLOW_ORIGIN, requestOrigin)
      } else {
        if (corsAllowOrigins.contains(requestOrigin)) response.headers.set(ACCESS_CONTROL_ALLOW_ORIGIN, requestOrigin)
      }
    }

    // Access-Control-Allow-Credentials
    if (!response.headers.contains(ACCESS_CONTROL_ALLOW_CREDENTIALS))
      response.headers.set(ACCESS_CONTROL_ALLOW_CREDENTIALS, true)

    // Access-Control-Allow-Methods
    if (!response.headers.contains(ACCESS_CONTROL_ALLOW_METHODS)) {
      val pathInfo = env.pathInfo
      if (pathInfo == null) {
        if (response.status == NOT_FOUND)
          response.headers.set(ACCESS_CONTROL_ALLOW_METHODS, OPTIONS.name)
        else
          response.headers.set(ACCESS_CONTROL_ALLOW_METHODS, OPTIONS.name + ", "+ GET.name + ", " + HEAD.name)
      } else {
        val allowMethods = OPTIONS +: Config.routes.tryAllMethods(pathInfo)
        response.headers.set(ACCESS_CONTROL_ALLOW_METHODS, allowMethods.mkString(", "))
      }
    }

    // Access-Control-Allow-Headers
    val accessControlRequestHeaders = request.headers.get(ACCESS_CONTROL_REQUEST_HEADERS)
    if (accessControlRequestHeaders != null && !response.headers.contains(ACCESS_CONTROL_ALLOW_HEADERS))
      response.headers.set(ACCESS_CONTROL_ALLOW_HEADERS, accessControlRequestHeaders)

    ctx.write(env, promise)
  }
}
