package xitrum.handler.up

import io.netty.channel.{ChannelHandler, SimpleChannelUpstreamHandler, ChannelHandlerContext, MessageEvent, Channels}
import io.netty.handler.codec.http.websocketx.{
  CloseWebSocketFrame, PingWebSocketFrame, PongWebSocketFrame, TextWebSocketFrame, WebSocketFrame, WebSocketServerHandshaker
}

import xitrum.Action

class WebSocketDispatcher(handshaker: WebSocketServerHandshaker, action: Action) extends SimpleChannelUpstreamHandler with BadClientSilencer {
  override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {
    val m = e.getMessage
    if (!m.isInstanceOf[WebSocketFrame]) {
      ctx.sendUpstream(e)
      return
    }

    val frame = m.asInstanceOf[WebSocketFrame]

    if (frame.isInstanceOf[CloseWebSocketFrame]) {
      handshaker.performClosingHandshake(ctx.getChannel, frame.asInstanceOf[CloseWebSocketFrame])
      action.onWebSocketClose
      return
    }

    if (frame.isInstanceOf[PingWebSocketFrame]) {
      ctx.getChannel.write(new PongWebSocketFrame(frame.getBinaryData))
      return
    }

    if (!(frame.isInstanceOf[TextWebSocketFrame])) {
      logger.warn("Frame type not supported: " + frame.getClass.getName)
      ctx.getChannel.close
      return
    }

    val text = frame.asInstanceOf[TextWebSocketFrame].getText
    action.onWebSocketFrame(text)
  }
}

