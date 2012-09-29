package xitrum.routing

import org.jboss.netty.handler.codec.http.HttpMethod

// To simplify the routing code, treat SockJS as a HttpMethod
object HttpMethodSockJs extends HttpMethod("SOCKJS")
