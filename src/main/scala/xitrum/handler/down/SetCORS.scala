package xitrum.handler.down

import org.jboss.netty.channel.{ChannelEvent, ChannelDownstreamHandler, ChannelHandler, ChannelHandlerContext, DownstreamMessageEvent}
import org.jboss.netty.handler.codec.http.{HttpHeaders, HttpMethod, HttpRequest, HttpResponse, HttpResponseStatus}
import ChannelHandler.Sharable
import HttpHeaders.Names._
import HttpMethod._
import HttpResponseStatus._

import xitrum.Config
import Config.xitrum.response.corsAllowOrigins
import xitrum.Log
import xitrum.handler.up.RequestAttacher
import xitrum.handler.{Attachment, HandlerEnv}

@Sharable
class SetCORS extends ChannelDownstreamHandler with Log {
  def handleDownstream(ctx: ChannelHandlerContext, e: ChannelEvent) {
    if (!e.isInstanceOf[DownstreamMessageEvent]) {
      ctx.sendDownstream(e)
      return
    }

    val m = e.asInstanceOf[DownstreamMessageEvent].getMessage
    if (!m.isInstanceOf[HttpResponse]) {
      ctx.sendDownstream(e)
      return
    }

    val request = RequestAttacher.retrieveOrSendDownstream(ctx, e)
    if (request == null) return

    val response = m.asInstanceOf[HttpResponse]

    // This is the last Xitrum handler, log the response
    if (log.isTraceEnabled) log.trace(response.toString)

    if (corsAllowOrigins.isEmpty) {
      ctx.sendDownstream(e)
      return
    }

    val requestOrigin = HttpHeaders.getHeader(request, ORIGIN)

    // Access-Control-Allow-Origin
    if (!response.headers.contains(ACCESS_CONTROL_ALLOW_ORIGIN)) {
      if (corsAllowOrigins(0).equals("*")) {
        if (requestOrigin == null || requestOrigin == "null")
          HttpHeaders.setHeader(response, ACCESS_CONTROL_ALLOW_ORIGIN, "*")
        else
          HttpHeaders.setHeader(response, ACCESS_CONTROL_ALLOW_ORIGIN, requestOrigin)
      } else {
        if (corsAllowOrigins.contains(requestOrigin))
          HttpHeaders.setHeader(response, ACCESS_CONTROL_ALLOW_ORIGIN, requestOrigin)
        else
          HttpHeaders.setHeader(response, ACCESS_CONTROL_ALLOW_ORIGIN, corsAllowOrigins.mkString(", "))
      }
    }

    // Access-Control-Allow-Credentials
    if (!response.headers.contains(ACCESS_CONTROL_ALLOW_CREDENTIALS))
      HttpHeaders.setHeader(response, ACCESS_CONTROL_ALLOW_CREDENTIALS, true)

    // Access-Control-Allow-Methods
    if (!response.headers.contains(ACCESS_CONTROL_ALLOW_METHODS)) {
      val attachment = ctx.getChannel.getAttachment.asInstanceOf[Attachment]
      if (attachment != null) {
        attachment.pathInfo match {
          case Some(pathInfo) =>
            val allowMethods = OPTIONS +: Config.routes.tryAllMethods(pathInfo)
            HttpHeaders.setHeader(response, ACCESS_CONTROL_ALLOW_METHODS, allowMethods.mkString(", "))

          case None =>
            if (response.getStatus == NOT_FOUND)
              HttpHeaders.setHeader(response, ACCESS_CONTROL_ALLOW_METHODS, OPTIONS.getName)
            else
              HttpHeaders.setHeader(response, ACCESS_CONTROL_ALLOW_METHODS, OPTIONS.getName + ", "+ GET.getName + ", " + HEAD.getName)
        }
      }
    }

    // Access-Control-Allow-Headers
    val accessControlRequestHeaders = HttpHeaders.getHeader(request, ACCESS_CONTROL_REQUEST_HEADERS)
    if (accessControlRequestHeaders != null && !response.headers.contains(ACCESS_CONTROL_ALLOW_HEADERS))
      HttpHeaders.setHeader(response, ACCESS_CONTROL_ALLOW_HEADERS, accessControlRequestHeaders)

    ctx.sendDownstream(e)
  }
}
