package xitrum.action

import java.net.InetSocketAddress
import org.jboss.netty.handler.codec.http.HttpHeaders.Names.HOST

trait Net {
  this: Action =>

  /**
   * @return IP of the HTTP client, X-Forwarded-For is supported
   *
   * See http://en.wikipedia.org/wiki/X-Forwarded-For
   *
   * TODO: see http://github.com/pepite/Play--Netty/blob/master/src/play/modules/netty/PlayHandler.java
   *
   * TODO: inetSocketAddress can be Inet4Address or Inet6Address
   * See java.net.preferIPv6Addresses
   */
  lazy val remoteIp = {
    val xRealIp = request.getHeader("X-Real-IP")
    if (xRealIp == null) {
      val inetSocketAddress = ctx.getChannel.getRemoteAddress.asInstanceOf[InetSocketAddress]
      inetSocketAddress.getAddress.getHostAddress
    } else {
      xRealIp
    }
  }

  lazy val ssl =
    henv.ssl ||
    request.getHeader("HTTPS") == "on" ||
    request.getHeader("HTTP_X_FORWARDED_PROTO") == "https"

  lazy val scheme = if (ssl) "https" else "http"

  lazy val (serverName, serverPort) = {
    val np = request.getHeader(HOST)  // Ex: localhost, localhost:3000
    val xs = np.split(":")
    if (xs.length == 1) {
      val port = if (ssl) 443 else 80
      (xs(0), port)
    } else {
      (xs(0), xs(1).toInt)
    }
  }

  lazy val contextPath = ""
}
