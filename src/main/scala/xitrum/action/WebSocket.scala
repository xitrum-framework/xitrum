package xitrum.action

import org.jboss.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory

import xitrum.Action
import xitrum.handler.DefaultHttpChannelPipelineFactory
import xitrum.handler.up.WebSocketDispatcher

/**
 * See https://github.com/netty/netty/blob/master/example/src/main/java/io/netty/example/http/websocketx/server/WebSocketServerHandler.java
 */
trait WebSocket {
  this: Action =>

  trait WebSocketHandler {
    def onOpen()

    /** Called when the websocket or the network connection is closed. */
    def onClose()

    /** Called when the client sends text data. */
    def onTextMessage(text: String)

    /** Called when the client sends binary data. */
    def onBinaryMessage(binary: Array[Byte])
  }

  /**
   * In the websocket entry point action, call this method if you want to accept
   * the connection.
   */
  def acceptWebSocket(handler: WebSocketHandler) {
    val factory    = new WebSocketServerHandshakerFactory(webSocketAbsRequestUrl, null, false)
    val handshaker = factory.newHandshaker(request)
    if (handshaker == null) {
      factory.sendUnsupportedWebSocketVersionResponse(channel)
    } else {
      handshaker.handshake(channel, request)

      val pipeline = channel.getPipeline
      DefaultHttpChannelPipelineFactory.removeUnusedForWebSocket(pipeline)
      pipeline.addLast("webSocketDispatcher", new WebSocketDispatcher(channel, handshaker, handler))

      handler.onOpen();
      channel.setReadable(true)  // Resume reading paused at NoPipelining
    }
  }
}
