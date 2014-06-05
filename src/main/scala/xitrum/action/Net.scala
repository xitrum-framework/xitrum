package xitrum.action

import java.net.{InetSocketAddress, SocketAddress}

import io.netty.handler.codec.http.{HttpHeaders, HttpRequest}
import io.netty.handler.ssl.SslHandler
import HttpHeaders.Names.HOST

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
object Net {
  // These are not put in trait Net so that they can be reused at AccessLog

  /** See reverseProxy in config/xitrum.conf */
  def proxyNotAllowed(clientIp: String): Boolean = {
    Config.xitrum.reverseProxy match {
      case None               => false
      case Some(reverseProxy) => !reverseProxy.ips.contains(clientIp)
    }
  }

  /** @return IP of the direct HTTP client (may be the proxy) */
  def clientIp(remoteAddress: SocketAddress): String = {
    // TODO: inetSocketAddress can be Inet4Address or Inet6Address
    // See java.net.preferIPv6Addresses
    val inetSocketAddress = remoteAddress.asInstanceOf[InetSocketAddress]
    val addr              = inetSocketAddress.getAddress
    addr.getHostAddress
  }

  /** @return IP of the HTTP client, X-Forwarded-For is supported */
  def remoteIp(remoteAddress: SocketAddress, request: HttpRequest): String = {
    val ip = clientIp(remoteAddress)

    if (proxyNotAllowed(ip)) {
      ip
    } else {
      val xForwardedFor = HttpHeaders.getHeader(request, "X-Forwarded-For")
      if (xForwardedFor == null)
        ip
      else
        xForwardedFor.split(",")(0).trim
    }
  }
}

trait Net {
  this: Action =>

  // The "val"s must be "lazy", because when the controller is constructed, the
  // "request" object is null

  /** See reverseProxy in config/xitrum.conf */
  private lazy val proxyNotAllowed = Net.proxyNotAllowed(clientIp)

  /** @return IPv4 or IPv6 of the direct HTTP client (may be the proxy) */
  private lazy val clientIp = Net.clientIp(channel.remoteAddress)

  /** @return IPv4 or IPv6 of the original remote HTTP client (not the proxy), X-Forwarded-For is supported */
  lazy val remoteIp = Net.remoteIp(channel.remoteAddress, request)

  lazy val isSsl = {
    if (channel.pipeline.get(classOf[SslHandler]) != null) {
      true
    } else {
      if (proxyNotAllowed) {
        false
      } else {
        val xForwardedProto = HttpHeaders.getHeader(request, "X-Forwarded-Proto")
        if (xForwardedProto != null)
          (xForwardedProto == "https")
        else {
          val xForwardedScheme = HttpHeaders.getHeader(request, "X-Forwarded-Scheme")
          if (xForwardedScheme != null)
            (xForwardedScheme == "https")
          else {
            val xForwardedSsl = HttpHeaders.getHeader(request, "X-Forwarded-Ssl")
            if (xForwardedSsl != null)
              (xForwardedSsl == "on")
            else
              false
          }
        }
      }
    }
  }

  lazy val scheme          = if (isSsl) "https" else "http"
  lazy val webSocketScheme = if (isSsl) "wss"   else "ws"

  lazy val (serverName, serverPort) = {
    val np = HttpHeaders.getHeader(request, HOST)  // Ex: localhost, localhost:3000
    val xs = np.split(':')
    if (xs.length == 1) {
      val port = if (isSsl) 443 else 80
      (xs(0), port)
    } else {
      (xs(0), xs(1).toInt)
    }
  }
}
