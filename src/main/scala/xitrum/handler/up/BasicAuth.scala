package xitrum.handler.up

import org.jboss.netty.buffer.ChannelBuffers
import org.jboss.netty.channel.{Channel, ChannelHandler, SimpleChannelUpstreamHandler, ChannelHandlerContext, MessageEvent}
import org.jboss.netty.handler.codec.http.{DefaultHttpResponse, HttpHeaders, HttpResponseStatus, HttpRequest, HttpResponse, HttpVersion}
import ChannelHandler.Sharable

import xitrum.Config
import xitrum.util.Base64

object BasicAuth {
  /** f takes username and password, and returns true if it want to let the user in. */
  def basicAuth(channel: Channel, request: HttpRequest, response: HttpResponse, realm: String)(f: (String, String) => Boolean): Boolean = {
    getUsernameAndPassword(request) match {
      case None =>
        respondBasic(channel, request, response, realm)
        false

      case Some((username, password)) =>
        if (f(username, password)) {
          true
        } else {
          respondBasic(channel, request, response, realm)
          false
        }
    }
  }

  private def getUsernameAndPassword(request: HttpRequest): Option[(String, String)] = {
    val authorization = request.getHeader(HttpHeaders.Names.AUTHORIZATION)
    if (authorization == null || !authorization.startsWith("Basic ")) {
      None
    } else {
      val username_password = authorization.substring(6)  // Skip "Basic "
      Base64.decode(username_password).flatMap { bytes =>
        val username_password2 = new String(bytes)
        val username_password3 = username_password2.split(':')
        if (username_password3.length != 2)
          None
        else
          Some((username_password3(0), username_password3(1)))
      }
    }
  }

  private def respondBasic(channel: Channel, request: HttpRequest, response: HttpResponse, realm: String) {
    response.setHeader(HttpHeaders.Names.WWW_AUTHENTICATE, "Basic realm=\"" + realm + "\"")
    response.setStatus(HttpResponseStatus.UNAUTHORIZED)

    val cb = ChannelBuffers.copiedBuffer("Wrong username or password", Config.requestCharset)
    response.setHeader(HttpHeaders.Names.CONTENT_TYPE, "text/plain; charset=" + Config.xitrum.request.charset)
    HttpHeaders.setContentLength(response, cb.readableBytes)
    response.setContent(cb)

    NoPipelining.setResponseHeaderForKeepAliveRequest(request, response)
    val future = channel.write(response)
    NoPipelining.if_keepAliveRequest_then_resumeReading_else_closeOnComplete(request, channel, future)
  }
}

@Sharable
class BasicAuth extends SimpleChannelUpstreamHandler with BadClientSilencer {
  override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {
    val m = e.getMessage
    if (!m.isInstanceOf[HttpRequest]) {
      ctx.sendUpstream(e)
      return
    }

    val go = Config.xitrum.basicAuth
    if (go.isEmpty) {
      ctx.sendUpstream(e)
      return
    }

    val channel  = ctx.getChannel
    val request  = m.asInstanceOf[HttpRequest]
    val response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.UNAUTHORIZED)
    val g        = go.get

    val passed = BasicAuth.basicAuth(channel, request, response, g.realm) { (username, password) =>
      g.username == username && g.password == password
    }
    if (passed) ctx.sendUpstream(e)
  }
}
