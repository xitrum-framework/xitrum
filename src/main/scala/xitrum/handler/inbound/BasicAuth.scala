package xitrum.handler.inbound

import io.netty.buffer.Unpooled
import io.netty.channel.{Channel, ChannelHandler, SimpleChannelInboundHandler, ChannelHandlerContext}
import io.netty.handler.codec.http.{DefaultHttpResponse, HttpHeaders, HttpResponseStatus, FullHttpRequest, FullHttpResponse, HttpVersion}
import ChannelHandler.Sharable

import xitrum.Config
import xitrum.handler.HandlerEnv
import xitrum.util.UrlSafeBase64

object BasicAuth {
  /** f takes username and password, and returns true if it want to let the user in. */
  def basicAuth(channel: Channel, request: FullHttpRequest, response: FullHttpResponse, realm: String)(f: (String, String) => Boolean): Boolean = {
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

  private def getUsernameAndPassword(request: FullHttpRequest): Option[(String, String)] = {
    val authorization = HttpHeaders.getHeader(request, HttpHeaders.Names.AUTHORIZATION)
    if (authorization == null || !authorization.startsWith("Basic ")) {
      None
    } else {
      val username_password = authorization.substring(6)  // Skip "Basic "
      UrlSafeBase64.autoPaddingDecode(username_password).flatMap { bytes =>
        val username_password2 = new String(bytes)
        val username_password3 = username_password2.split(':')
        if (username_password3.length != 2)
          None
        else
          Some((username_password3(0), username_password3(1)))
      }
    }
  }

  private def respondBasic(channel: Channel, request: FullHttpRequest, response: FullHttpResponse, realm: String) {
    HttpHeaders.setHeader(response, HttpHeaders.Names.WWW_AUTHENTICATE, "Basic realm=\"" + realm + "\"")
    response.setStatus(HttpResponseStatus.UNAUTHORIZED)

    val cb = "Wrong username or password".getBytes(Config.xitrum.request.charset)
    HttpHeaders.setHeader(response, HttpHeaders.Names.CONTENT_TYPE, "text/plain; charset=" + Config.xitrum.request.charset)
    HttpHeaders.setContentLength(response, cb.length)
    response.content.writeBytes(cb)

    NoPipelining.setResponseHeaderForKeepAliveRequest(request, response)
    val future = channel.writeAndFlush(response)
    NoPipelining.if_keepAliveRequest_then_resumeReading_else_closeOnComplete(request, channel, future)
  }
}

@Sharable
class BasicAuth extends SimpleChannelInboundHandler[HandlerEnv] {
  override def channelRead0(ctx: ChannelHandlerContext, env: HandlerEnv) {
    val go = Config.xitrum.basicAuth
    if (go.isEmpty) {
      ctx.fireChannelRead(env)
      return
    }

    val g      = go.get
    val passed = BasicAuth.basicAuth(env.channel, env.request, env.response, g.realm) { (username, password) =>
      g.username == username && g.password == password
    }
    if (passed) ctx.fireChannelRead(env)
  }
}
