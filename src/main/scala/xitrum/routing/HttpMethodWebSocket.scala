package xitrum.routing

import org.jboss.netty.handler.codec.http.HttpMethod

// To simplify the routing code, treat WebSocket as a HttpMethod
object HttpMethodWebSocket extends HttpMethod("WEBSOCKET")
