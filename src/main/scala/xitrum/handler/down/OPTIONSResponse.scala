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
import xitrum.handler.up.RequestAttacher
import xitrum.handler.{AccessLog, Attachment, HandlerEnv}

@Sharable
class OPTIONSResponse extends ChannelDownstreamHandler {
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

    if (request.getMethod == OPTIONS) {
      AccessLog.logOPTIONS(request)
      val response = m.asInstanceOf[HttpResponse]
      val attachment = ctx.getChannel.getAttachment.asInstanceOf[Attachment]
      if (attachment != null) {
        attachment.pathInfo match {
          // Case of dynamic resources.
          case Some(pathInfo) =>
            if (!Config.routes.tryAllMethods(pathInfo).isEmpty)
              response.setStatus(NO_CONTENT)
            else
              response.setStatus(NOT_FOUND)

          // Case of static files/resources.
          case None =>
            if (response.getStatus != NOT_FOUND) response.setStatus(NO_CONTENT)
        }
      }
      HttpHeaders.setContentLength(response, 0)
      NotModified.setClientCacheAggressively(response)
      response.setContent(ChannelBuffers.EMPTY_BUFFER)
    }
    ctx.sendDownstream(e)
  }
}
