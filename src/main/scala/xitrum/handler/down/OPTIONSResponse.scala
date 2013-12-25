package xitrum.handler.down

import org.jboss.netty.buffer.ChannelBuffers
import org.jboss.netty.channel.{ChannelEvent, ChannelDownstreamHandler, ChannelHandler, ChannelHandlerContext, DownstreamMessageEvent}
import org.jboss.netty.handler.codec.http.{DefaultHttpResponse, HttpHeaders, HttpMethod, HttpRequest, HttpResponse, HttpResponseStatus}
import ChannelHandler.Sharable
import HttpHeaders.Names._
import HttpMethod._
import HttpResponseStatus._

import xitrum.Config
import xitrum.etag.NotModified
import xitrum.handler.{AccessLog, HandlerEnv}

@Sharable
class OPTIONSResponse extends ChannelDownstreamHandler {
  def handleDownstream(ctx: ChannelHandlerContext, e: ChannelEvent) {
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
    val pathInfo = env.pathInfo

    if (request.getMethod == OPTIONS) {
      AccessLog.logOPTIONS(request)

      if (pathInfo == null) {
        // Static files/resources
        if (response.getStatus != NOT_FOUND) response.setStatus(NO_CONTENT)
      } else {
        // Dynamic resources
        if (!Config.routes.tryAllMethods(pathInfo).isEmpty)
          response.setStatus(NO_CONTENT)
        else
          response.setStatus(NOT_FOUND)
      }

      HttpHeaders.setContentLength(response, 0)
      NotModified.setClientCacheAggressively(response)
      response.setContent(ChannelBuffers.EMPTY_BUFFER)
    }

    ctx.sendDownstream(e)
  }
}
