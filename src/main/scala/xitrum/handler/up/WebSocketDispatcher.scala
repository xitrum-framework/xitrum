package xitrum.handler.up

import org.jboss.netty.channel.{
  Channel, ChannelHandler, ChannelHandlerContext, ChannelFuture,
  ChannelFutureListener, SimpleChannelUpstreamHandler, MessageEvent
}
import org.jboss.netty.handler.codec.http.websocketx.{
  BinaryWebSocketFrame, CloseWebSocketFrame, PingWebSocketFrame, PongWebSocketFrame,
  TextWebSocketFrame, WebSocketFrame, WebSocketServerHandshaker
}

import xitrum.action.WebSocket
import xitrum.util.ChannelBufferToBytes

/** See https://github.com/netty/netty/blob/master/example/src/main/java/io/netty/example/http/websocketx/server/WebSocketServerHandler.java */
class WebSocketDispatcher(
    channel:    Channel,
    handshaker: WebSocketServerHandshaker,
    handler:    WebSocket#WebSocketHandler
) extends SimpleChannelUpstreamHandler with BadClientSilencer
{
  // Prevent WebSocketHandler#onClosed to be called twice
  private var onClosedCalled: Boolean = false

  private def callOnClose(): Unit = synchronized {
    if (!onClosedCalled) {
      onClosedCalled = true
      handler.onClose()
    }
  }

  channel.getCloseFuture.addListener(new ChannelFutureListener {
    def operationComplete(future: ChannelFuture) { callOnClose() }
  })

  override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {
    val m = e.getMessage
    if (!m.isInstanceOf[WebSocketFrame]) {
      ctx.sendUpstream(e)
      return
    }

    val frame = m.asInstanceOf[WebSocketFrame]

    if (frame.isInstanceOf[CloseWebSocketFrame]) {
      handshaker.close(ctx.getChannel, frame.asInstanceOf[CloseWebSocketFrame]).addListener(ChannelFutureListener.CLOSE)
      callOnClose()
      return
    }

    if (frame.isInstanceOf[PingWebSocketFrame]) {
      ctx.getChannel.write(new PongWebSocketFrame(frame.getBinaryData))
      return
    }

    if (frame.isInstanceOf[TextWebSocketFrame]) {
      val text = frame.asInstanceOf[TextWebSocketFrame].getText
      handler.onTextMessage(text)
      return
    }

    if (frame.isInstanceOf[BinaryWebSocketFrame]) {
      val binary = ChannelBufferToBytes(frame.asInstanceOf[BinaryWebSocketFrame].getBinaryData)
      handler.onBinaryMessage(binary)
      return
    }
  }
}

