package xitrum.handler.inbound

import io.netty.buffer.Unpooled
import io.netty.channel.{ChannelHandler, SimpleChannelInboundHandler, ChannelHandlerContext}
import io.netty.handler.codec.http.{HttpHeaders, HttpResponseStatus, FullHttpRequest}
import ChannelHandler.Sharable

import xitrum.Config
import xitrum.handler.{HandlerEnv, NoRealPipelining}
import xitrum.util.{ByteBufUtil, SeriDeseri}

object BasicAuth {
  /** f takes username and password, and returns true if it want to let the user in. */
  def basicAuth(env: HandlerEnv, realm: String)(f: (String, String) => Boolean): Boolean = {
    getUsernameAndPassword(env.request) match {
      case None =>
        respondBasic(env, realm)
        false

      case Some((username, password)) =>
        if (f(username, password)) {
          true
        } else {
          respondBasic(env, realm)
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
      SeriDeseri.bytesFromUrlSafeBase64(username_password).flatMap { bytes =>
        val username_password2 = new String(bytes)
        val username_password3 = username_password2.split(':')
        if (username_password3.length != 2)
          None
        else
          Some((username_password3(0), username_password3(1)))
      }
    }
  }

  private def respondBasic(env: HandlerEnv, realm: String) {
    val request  = env.request
    val response = env.response

    HttpHeaders.setHeader(response, HttpHeaders.Names.WWW_AUTHENTICATE, "Basic realm=\"" + realm + "\"")
    response.setStatus(HttpResponseStatus.UNAUTHORIZED)

    HttpHeaders.setHeader(response, HttpHeaders.Names.CONTENT_TYPE, "text/plain; charset=" + Config.xitrum.request.charset)
    ByteBufUtil.writeComposite(
      response.content,
      Unpooled.copiedBuffer("Wrong username or password", Config.xitrum.request.charset)
    )

    val future = env.channel.writeAndFlush(env)
    NoRealPipelining.if_keepAliveRequest_then_resumeReading_else_closeOnComplete(request, env.channel, future)
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
    val passed = BasicAuth.basicAuth(env, g.realm) { (username, password) =>
      g.username == username && g.password == password
    }
    if (passed) ctx.fireChannelRead(env)
  }
}
