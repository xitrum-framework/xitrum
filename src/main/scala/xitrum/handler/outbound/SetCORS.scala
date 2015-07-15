package xitrum.handler.outbound

import io.netty.channel.{ChannelHandler, ChannelHandlerContext, ChannelOutboundHandlerAdapter, ChannelPromise}
import io.netty.handler.codec.http.{HttpHeaders, HttpMethod, HttpResponseStatus}
import ChannelHandler.Sharable
import HttpHeaders.Names._
import HttpMethod._
import HttpResponseStatus._

import xitrum.Config
import xitrum.handler.HandlerEnv

@Sharable
class SetCORS extends ChannelOutboundHandlerAdapter {
  override def write(ctx: ChannelHandlerContext, msg: Object, promise: ChannelPromise) {
    import Config.xitrum.response.corsAllowOrigins

    if (!msg.isInstanceOf[HandlerEnv] || corsAllowOrigins.isEmpty) {
      ctx.write(msg, promise)
      return
    }

    val env      = msg.asInstanceOf[HandlerEnv]
    val request  = env.request
    val response = env.response

    val requestOrigin = HttpHeaders.getHeader(request, ORIGIN)

    // Access-Control-Allow-Origin
    if (!response.headers.contains(ACCESS_CONTROL_ALLOW_ORIGIN)) {
      if (corsAllowOrigins.head.equals("*")) {
        if (requestOrigin == null || requestOrigin == "null")
          HttpHeaders.setHeader(response, ACCESS_CONTROL_ALLOW_ORIGIN, "*")
        else
          HttpHeaders.setHeader(response, ACCESS_CONTROL_ALLOW_ORIGIN, requestOrigin)
      } else {
        if (corsAllowOrigins.contains(requestOrigin)) HttpHeaders.setHeader(response, ACCESS_CONTROL_ALLOW_ORIGIN, requestOrigin)
      }
    }

    // Access-Control-Allow-Credentials
    if (!response.headers.contains(ACCESS_CONTROL_ALLOW_CREDENTIALS))
      HttpHeaders.setHeader(response, ACCESS_CONTROL_ALLOW_CREDENTIALS, true)

    // Access-Control-Allow-Methods
    if (!response.headers.contains(ACCESS_CONTROL_ALLOW_METHODS)) {
      val pathInfo = env.pathInfo
      if (pathInfo == null) {
        if (response.getStatus == NOT_FOUND)
          HttpHeaders.setHeader(response, ACCESS_CONTROL_ALLOW_METHODS, OPTIONS.name)
        else
          HttpHeaders.setHeader(response, ACCESS_CONTROL_ALLOW_METHODS, OPTIONS.name + ", "+ GET.name + ", " + HEAD.name)
      } else {
        val allowMethods = OPTIONS +: Config.routes.tryAllMethods(pathInfo)
        HttpHeaders.setHeader(response, ACCESS_CONTROL_ALLOW_METHODS, allowMethods.mkString(", "))
      }
    }

    // Access-Control-Allow-Headers
    val accessControlRequestHeaders = HttpHeaders.getHeader(request, ACCESS_CONTROL_REQUEST_HEADERS)
    if (accessControlRequestHeaders != null && !response.headers.contains(ACCESS_CONTROL_ALLOW_HEADERS))
      HttpHeaders.setHeader(response, ACCESS_CONTROL_ALLOW_HEADERS, accessControlRequestHeaders)

    ctx.write(env, promise)
  }
}
