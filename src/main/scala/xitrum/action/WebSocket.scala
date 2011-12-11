package xitrum.action

import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory

import xitrum.Action
import xitrum.handler.ChannelPipelineFactory
import xitrum.handler.up.WebSocketDispatcher

trait WebSocket {
  this: Action =>

  def webSocketHandshake {
    val url     = webSocketScheme + "://" + serverName + ":" + serverPort + request.getUri
    val factory = new WebSocketServerHandshakerFactory(url, null, false)
    val handshaker = factory.newHandshaker(request)
    if (handshaker == null) {
      factory.sendUnsupportedWebSocketVersionResponse(channel)
    } else {
      handshaker.performOpeningHandshake(channel, request)
      val pipeline = channel.getPipeline
      ChannelPipelineFactory.removeUnusedDefaultHttpHandlersForWebSocket(pipeline)
      pipeline.addLast("webSocketDispatcher", new WebSocketDispatcher(handshaker, this))
    }
  }

  def onWebSocketFrame(text: String) {}
  def onWebSocketClose {}
}
