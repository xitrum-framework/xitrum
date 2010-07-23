package xt.server

import scala.collection.mutable.HashMap

import org.jboss.netty.channel.{SimpleChannelUpstreamHandler,
                                ChannelHandlerContext,
                                MessageEvent,
                                ExceptionEvent,
                                ChannelFutureListener}
import org.jboss.netty.handler.codec.http.{HttpRequest,
                                           DefaultHttpResponse,
                                           HttpResponse}
import org.jboss.netty.handler.codec.http.HttpHeaders._
import org.jboss.netty.handler.codec.http.HttpHeaders.Names._
import org.jboss.netty.handler.codec.http.HttpResponseStatus._
import org.jboss.netty.handler.codec.http.HttpVersion._

import xt.middleware.App

class Handler(app: App) extends SimpleChannelUpstreamHandler {
  override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {
    val m = e.getMessage
    if (m.isInstanceOf[HttpRequest]) {
      val channel  = e.getChannel
      val request  = m.asInstanceOf[HttpRequest]
      val response = new DefaultHttpResponse(HTTP_1_1, OK)
      val env      = new HashMap[String, Any]

      app.call(channel, request, response, env)
      if (env.get("bypass_respond").isEmpty) respond(e, request, response)
    }
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, e: ExceptionEvent) {
    error(e.toString)
    e.getChannel.close
  }

  //----------------------------------------------------------------------------

  private def respond(e: MessageEvent, request: HttpRequest, response: HttpResponse) {
    val keepAlive = isKeepAlive(request)

    // Add 'Content-Length' header only for a keep-alive connection.
    // Close the non-keep-alive connection after the write operation is done.
    if (keepAlive) {
      response.setHeader(CONTENT_LENGTH, response.getContent.readableBytes)
    }
    val future = e.getChannel.write(response)
    if (!keepAlive) {
      future.addListener(ChannelFutureListener.CLOSE)
    }
  }
}
