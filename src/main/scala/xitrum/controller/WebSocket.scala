package xitrum.controller

import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory

import xitrum.Controller
import xitrum.handler.ChannelPipelineFactory
import xitrum.handler.up.WebSocketDispatcher

trait WebSocket {
  this: Controller =>

  def webSocketHandshake() {
    val url     = webSocketScheme + "://" + serverName + ":" + serverPort + request.getUri
    val factory = new WebSocketServerHandshakerFactory(url, null, false)
    val handshaker = factory.newHandshaker(request)
    if (handshaker == null) {
      factory.sendUnsupportedWebSocketVersionResponse(channel)
    } else {
      handshaker.handshake(channel, request)
      val pipeline = channel.getPipeline
      ChannelPipelineFactory.removeUnusedDefaultHttpHandlersForWebSocket(pipeline)
      pipeline.addLast("webSocketDispatcher", new WebSocketDispatcher(handshaker, this))
    }
  }

  def onWebSocketFrame(text: String) {}
  def onWebSocketClose() {}
}
