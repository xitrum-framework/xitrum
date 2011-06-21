package xitrum.action

import java.net.InetSocketAddress
import org.jboss.netty.handler.codec.http.HttpHeaders.Names.HOST

import xitrum.{Action, Config}

// See:
//   http://httpd.apache.org/docs/2.2/mod/mod_proxy.html
//   http://en.wikipedia.org/wiki/X-Forwarded-For
//   https://github.com/playframework/play/blob/master/framework/src/play/mvc/Http.java
//   https://github.com/pepite/Play--Netty/blob/master/src/play/modules/netty/PlayHandler.java
//
// X-Forwarded-Host:   the original HOST header
// X-Forwarded-For:    comma separated, the first element is the origin
// X-Forwarded-Proto:  http/https
// X-Forwarded-Scheme: same as X-Forwarded-Proto
// X-Forwarded-Ssl:    on/off
//
// Note:
//   X-Forwarded-Server is the hostname of the proxy server, not the same as
//   X-Forwarded-Host.
trait Net {
  this: Action =>

/* Play
        if (Play.configuration.containsKey("XForwardedSupport") && nettyRequest.getHeader("X-Forwarded-For") != null) {
            if (!Arrays.asList(Play.configuration.getProperty("XForwardedSupport", "127.0.0.1").split(',')).contains(request.remoteAddress)) {
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

  /** @return IP of the HTTP client, X-Forwarded-For is supported */
  lazy val remoteIp = {
    if (proxyNotAllowed)
      clientIp
    else {
      val xForwardedFor = request.getHeader("X-Forwarded-For")
      if (xForwardedFor == null)
        clientIp
      else
        xForwardedFor.split(",")(0).trim
    }
  }

  lazy val isSsl = {
    if (proxyNotAllowed)
      false
    else {
      val xForwardedProto = request.getHeader("X-Forwarded-Proto")
      if (xForwardedProto != null)
        (xForwardedProto == "https")
      else {
        val xForwardedScheme = request.getHeader("X-Forwarded-Scheme")
        if (xForwardedScheme != null) 
          (xForwardedScheme == "https")
        else {
          val xForwardedSsl = request.getHeader("X-Forwarded-Ssl")
          if (xForwardedSsl != null)
            (xForwardedSsl == "on")
          else
            false
        }
      }
    }
  }

  lazy val scheme = if (isSsl) "https" else "http"

  lazy val (serverName, serverPort) = {
    val np = request.getHeader(HOST)  // Ex: localhost, localhost:3000
    val xs = np.split(':')
    if (xs.length == 1) {
      val port = if (isSsl) 443 else 80
      (xs(0), port)
    } else
      (xs(0), xs(1).toInt)
  }

  lazy val absoluteUrlPrefix = {
    val portSuffix =
      if ((isSsl && serverPort == 443) || (!isSsl && serverPort == 80))
        ""
      else
        ":" + serverPort
    scheme + "://" + serverName + portSuffix
  }

  //---------------------------------------------------------------------------

  // TODO: inetSocketAddress can be Inet4Address or Inet6Address
  // See java.net.preferIPv6Addresses
  private lazy val clientIp = {
    val inetSocketAddress = ctx.getChannel.getRemoteAddress.asInstanceOf[InetSocketAddress]
    inetSocketAddress.getAddress.getHostAddress
  }

  private lazy val proxyNotAllowed = {
    Config.proxyIpso match {
      case None      => false
      case Some(ips) => ips.contains(clientIp)
    }
  }
}
