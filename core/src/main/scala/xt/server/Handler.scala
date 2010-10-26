package xt.server

import scala.collection.mutable.HashMap

import org.jboss.netty.channel.{Channel,
                              SimpleChannelUpstreamHandler,
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

import xt._
import xt.middleware.App

object Handler {
  val IGNORE_RESPONSE = "HANDLER_SHOULD_IGNORE_THE_RESPONSE"

  /**
   * One may do asynchronous responding by setting IGNORE_RESPONSE to the "env"
   * so that the automatic response is ignored. Then later, use this function to
   * manually respond to the client.
   */
  def respond(channel: Channel, request: HttpRequest, response: HttpResponse) {
    val keepAlive = isKeepAlive(request)

    // Add 'Content-Length' header only for a keep-alive connection.
    // Close the non-keep-alive connection after the write operation is done.
    if (keepAlive) {
      response.setHeader(CONTENT_LENGTH, response.getContent.readableBytes)
    }
    val future = channel.write(response)
    if (!keepAlive) {
      future.addListener(ChannelFutureListener.CLOSE)
    }
  }
}

class Handler(app: App) extends SimpleChannelUpstreamHandler {
  import Handler._

  override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {
    val m = e.getMessage
    if (m.isInstanceOf[HttpRequest]) {
      val channel  = e.getChannel
      val request  = m.asInstanceOf[HttpRequest]
      val response = new DefaultHttpResponse(HTTP_1_1, OK)
      val env      = new HashMap[String, Any]

      app.call(channel, request, response, env)
      if (!env.contains(IGNORE_RESPONSE)) respond(channel, request, response)
    }
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, e: ExceptionEvent) {
    logger.error("Exception at xt.server.Handler", e.getCause)
    e.getChannel.close
  }
}
