package xitrum.action

import java.net.InetSocketAddress
import org.jboss.netty.handler.codec.http.HttpHeaders.Names.HOST

trait Net {
  this: Action =>

/* Play
        if (Play.configuration.containsKey("XForwardedSupport") && nettyRequest.getHeader("X-Forwarded-For") != null) {
            if (!Arrays.asList(Play.configuration.getProperty("XForwardedSupport", "127.0.0.1").split(",")).contains(request.remoteAddress)) {
                throw new RuntimeException("This proxy request is not authorized: " + request.remoteAddress);
            } else {
                request.secure = ("https".equals(Play.configuration.get("XForwardedProto")) || "https".equals(nettyRequest.getHeader("X-Forwarded-Proto")) || "on".equals(nettyRequest.getHeader("X-Forwarded-Ssl")));
                if (Play.configuration.containsKey("XForwardedHost")) {
                    request.host = (String) Play.configuration.get("XForwardedHost");
                } else if (nettyRequest.getHeader("X-Forwarded-Host") != null) {
                    request.host = nettyRequest.getHeader("X-Forwarded-Host");
                }
                if (nettyRequest.getHeader("X-Forwarded-For") != null) {
                    request.remoteAddress = nettyRequest.getHeader("X-Forwarded-For");
                }
            }
        }

*/

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

  lazy val isSsl =
    henv.ssl ||
    request.getHeader("HTTPS") == "on" ||
    request.getHeader("HTTP_X_FORWARDED_PROTO") == "https"

  lazy val scheme = if (isSsl) "https" else "http"

  lazy val (serverName, serverPort) = {
    val np = request.getHeader(HOST)  // Ex: localhost, localhost:3000
    val xs = np.split(":")
    if (xs.length == 1) {
      val port = if (isSsl) 443 else 80
      (xs(0), port)
    } else {
      (xs(0), xs(1).toInt)
    }
  }

  lazy val contextPath = ""
}
