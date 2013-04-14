package xitrum.handler.up

import akka.actor.ActorRef

import org.jboss.netty.channel.{
  Channel, ChannelHandler, ChannelHandlerContext, ChannelFuture,
  ChannelFutureListener, SimpleChannelUpstreamHandler, MessageEvent
}
import org.jboss.netty.handler.codec.http.websocketx.{
  BinaryWebSocketFrame, CloseWebSocketFrame, PingWebSocketFrame, PongWebSocketFrame,
  TextWebSocketFrame, WebSocketFrame, WebSocketServerHandshaker
}

import xitrum.{WebSocketBinary, WebSocketPing, WebSocketPong, WebSocketText}
import xitrum.util.ChannelBufferToBytes

/** See https://github.com/netty/netty/blob/master/example/src/main/java/io/netty/example/http/websocketx/server/WebSocketServerHandler.java */
class WebSocketEventDispatcher(
    handshaker: WebSocketServerHandshaker,
    actorRef:   ActorRef
) extends SimpleChannelUpstreamHandler with BadClientSilencer
{
  override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {
    val m = e.getMessage
    if (!m.isInstanceOf[WebSocketFrame]) {
      ctx.sendUpstream(e)
      return
    }

    val frame = m.asInstanceOf[WebSocketFrame]

    if (frame.isInstanceOf[TextWebSocketFrame]) {
      val text = frame.asInstanceOf[TextWebSocketFrame].getText
      actorRef ! WebSocketText(text)
      return
    }

    if (frame.isInstanceOf[BinaryWebSocketFrame]) {
      val bytes = ChannelBufferToBytes(frame.asInstanceOf[BinaryWebSocketFrame].getBinaryData)
      actorRef ! WebSocketBinary(bytes)
      return
    }

    if (frame.isInstanceOf[PingWebSocketFrame]) {
      ctx.getChannel.write(new PongWebSocketFrame(frame.getBinaryData))
      actorRef ! WebSocketPing
      return
    }

    if (frame.isInstanceOf[PongWebSocketFrame]) {
      actorRef ! WebSocketPong
      return
    }

    if (frame.isInstanceOf[CloseWebSocketFrame]) {
      handshaker.close(ctx.getChannel, frame.asInstanceOf[CloseWebSocketFrame]).addListener(ChannelFutureListener.CLOSE)
      return
    }
  }
}

