package xt.server

import xt._
import xt.middleware.App

import java.net.SocketAddress
import scala.collection.mutable.{Map => MMap, HashMap}

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

object Handler {
  val IGNORE_RESPONSE = "HANDLER_SHOULD_IGNORE_THE_RESPONSE"

  def ignoreResponse(env: MMap[String, Any]) {
    env.put(IGNORE_RESPONSE, true)
  }

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

class Handler(app: App) extends SimpleChannelUpstreamHandler with Logger {
  import Handler._

  override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {
    val m = e.getMessage
    if (m.isInstanceOf[HttpRequest]) {
      val request  = m.asInstanceOf[HttpRequest]
      val address  = e.getRemoteAddress
      val remoteIp = getRemoteIp(address, request)
      val channel  = e.getChannel

      val response = new DefaultHttpResponse(HTTP_1_1, OK)
      val env      = new HashMap[String, Any]

      app.call(remoteIp, channel, request, response, env)
      if (!env.contains(IGNORE_RESPONSE)) respond(channel, request, response)
    }
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, e: ExceptionEvent) {
    logger.error("xt.server.Handler", e.getCause)
    e.getChannel.close
  }

  //----------------------------------------------------------------------------

  /**
   * @return IP of the HTTP client, X-Forwarded-For is supported
   *
   * See http://en.wikipedia.org/wiki/X-Forwarded-For is suppored
   */
  private def getRemoteIp(socketAddress: SocketAddress, request: HttpRequest): String = {
    // TODO: see http://github.com/pepite/Play--Netty/blob/master/src/play/modules/netty/PlayHandler.java
    val remoteAddress = socketAddress.toString
    val ret = remoteAddress.substring(1, remoteAddress.indexOf(':'))
    ret
  }
}
