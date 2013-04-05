package xitrum.action

import org.jboss.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory

import xitrum.ActionEnv
import xitrum.handler.ChannelPipelineFactory
import xitrum.handler.up.WebSocketDispatcher

/**
 * See https://github.com/netty/netty/blob/master/example/src/main/java/io/netty/example/http/websocketx/server/WebSocketServerHandler.java
 */
trait WebSocket {
  this: ActionEnv =>

  trait WebSocketHandler {
    def onOpen()

    /** Called when the websocket or the network connection is closed. */
    def onClose()

    /**
     * Called when the client sends data (only text is supported).
     * You may call respondWebSocket(string) to send data to the client.
     */
    def onMessage(text: String)
  }

  /**
   * In the websocket entry point action, call this method if you want to accept
   * the connection.
   */
  def acceptWebSocket(handler: WebSocketHandler) {
    val factory    = new WebSocketServerHandshakerFactory(webSocketAbsoluteRequestUrl, null, false)
    val handshaker = factory.newHandshaker(request)
    if (handshaker == null) {
      factory.sendUnsupportedWebSocketVersionResponse(channel)
    } else {
      handshaker.handshake(channel, request)

      val pipeline = channel.getPipeline
      ChannelPipelineFactory.removeUnusedDefaultHttpHandlersForWebSocket(pipeline)
      pipeline.addLast("webSocketDispatcher", new WebSocketDispatcher(handshaker, handler))

      handler.onOpen();
      channel.setReadable(true)  // Resume reading paused at NoPipelining
    }
  }
}
