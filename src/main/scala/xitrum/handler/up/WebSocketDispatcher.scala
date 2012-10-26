package xitrum.handler.up

import org.jboss.netty.channel.{ChannelHandler, ChannelHandlerContext, ChannelFuture, ChannelFutureListener, SimpleChannelUpstreamHandler, MessageEvent}
import org.jboss.netty.handler.codec.http.websocketx.{
  CloseWebSocketFrame, PingWebSocketFrame, PongWebSocketFrame, TextWebSocketFrame, WebSocketFrame, WebSocketServerHandshaker
}

import xitrum.controller.WebSocket

/**
 * See https://github.com/netty/netty/blob/master/example/src/main/java/io/netty/example/http/websocketx/server/WebSocketServerHandler.java
 */
class WebSocketDispatcher(handshaker: WebSocketServerHandshaker, handler: WebSocket#WebSocketHandler) extends SimpleChannelUpstreamHandler with BadClientSilencer {
  // Prevent WebSocketHandler#onClosed to be called twice
  private var onClosedCalled: Boolean = false

  private def callOnClose(): Unit = synchronized {
    if (!onClosedCalled) {
      onClosedCalled = true
      handler.onClose()
    }
  }

  override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {
    val m = e.getMessage
    if (!m.isInstanceOf[WebSocketFrame]) {
      ctx.sendUpstream(e)
      return
    }

    val frame = m.asInstanceOf[WebSocketFrame]
    if (frame.isInstanceOf[CloseWebSocketFrame]) {
      handshaker.close(ctx.getChannel, frame.asInstanceOf[CloseWebSocketFrame])
      callOnClose()
      return
    }

    ctx.getChannel.getCloseFuture.addListener(new ChannelFutureListener {
      def operationComplete(future: ChannelFuture) { callOnClose() }
    })

    if (frame.isInstanceOf[PingWebSocketFrame]) {
      ctx.getChannel.write(new PongWebSocketFrame(frame.getBinaryData))
      return
    }

    if (!frame.isInstanceOf[TextWebSocketFrame]) {
      logger.warn("WebSocket frame type not supported: " + frame.getClass.getName)
      ctx.getChannel.close()
      return
    }

    val text = frame.asInstanceOf[TextWebSocketFrame].getText
    handler.onMessage(text)
  }
}

