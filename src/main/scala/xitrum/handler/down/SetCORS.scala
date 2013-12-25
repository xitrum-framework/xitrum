package xitrum.handler.down

import org.jboss.netty.channel.{ChannelEvent, ChannelDownstreamHandler, ChannelHandler, ChannelHandlerContext, DownstreamMessageEvent}
import org.jboss.netty.handler.codec.http.{HttpHeaders, HttpMethod, HttpRequest, HttpResponse, HttpResponseStatus}
import ChannelHandler.Sharable
import HttpHeaders.Names._
import HttpMethod._
import HttpResponseStatus._

import xitrum.Config
import xitrum.handler.HandlerEnv

@Sharable
class SetCORS extends ChannelDownstreamHandler {
  def handleDownstream(ctx: ChannelHandlerContext, e: ChannelEvent) {
    import Config.xitrum.response.corsAllowOrigins

    if (corsAllowOrigins.isEmpty) {
      ctx.sendDownstream(e)
      return
    }

    if (!e.isInstanceOf[DownstreamMessageEvent]) {
      ctx.sendDownstream(e)
      return
    }

    val m = e.asInstanceOf[DownstreamMessageEvent].getMessage
    if (!m.isInstanceOf[HandlerEnv]) {
      ctx.sendDownstream(e)
      return
    }

    val env      = m.asInstanceOf[HandlerEnv]
    val request  = env.request
    val response = env.response

    val requestOrigin = HttpHeaders.getHeader(request, ORIGIN)

    // Access-Control-Allow-Origin
    if (!response.headers.contains(ACCESS_CONTROL_ALLOW_ORIGIN)) {
      if (corsAllowOrigins(0).equals("*")) {
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
          HttpHeaders.setHeader(response, ACCESS_CONTROL_ALLOW_METHODS, OPTIONS.getName)
        else
          HttpHeaders.setHeader(response, ACCESS_CONTROL_ALLOW_METHODS, OPTIONS.getName + ", "+ GET.getName + ", " + HEAD.getName)
      } else {
        val allowMethods = OPTIONS +: Config.routes.tryAllMethods(pathInfo)
        HttpHeaders.setHeader(response, ACCESS_CONTROL_ALLOW_METHODS, allowMethods.mkString(", "))
      }
    }

    // Access-Control-Allow-Headers
    val accessControlRequestHeaders = HttpHeaders.getHeader(request, ACCESS_CONTROL_REQUEST_HEADERS)
    if (accessControlRequestHeaders != null && !response.headers.contains(ACCESS_CONTROL_ALLOW_HEADERS))
      HttpHeaders.setHeader(response, ACCESS_CONTROL_ALLOW_HEADERS, accessControlRequestHeaders)

    ctx.sendDownstream(e)
  }
}
