package xt.server

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
      val req = m.asInstanceOf[HttpRequest]
      val res = new DefaultHttpResponse(HTTP_1_1, OK)
      res.setHeader(CONTENT_TYPE, "text/plain")
      app.handle(req, res)
      respond(e, req, res)
    }
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, e: ExceptionEvent) {
    error(e.toString)
    e.getChannel.close
  }

  //----------------------------------------------------------------------------

  private def respond(e: MessageEvent, req: HttpRequest, res: HttpResponse) {
    val keepAlive = isKeepAlive(req)

    // Add 'Content-Length' header only for a keep-alive connection.
    // Close the non-keep-alive connection after the write operation is done.
    if (keepAlive) {
      res.setHeader(CONTENT_LENGTH, res.getContent.readableBytes)
    }
    val future = e.getChannel.write(res)
    if (!keepAlive) {
      future.addListener(ChannelFutureListener.CLOSE)
    }
  }
}
