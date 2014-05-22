package xitrum.handler.inbound

import scala.runtime.ScalaRunTime
import akka.actor.ActorRef

import io.netty.channel.{
  Channel, ChannelHandler, ChannelHandlerContext, ChannelFuture,
  ChannelFutureListener, SimpleChannelInboundHandler
}
import io.netty.handler.codec.http.websocketx.{
  BinaryWebSocketFrame, CloseWebSocketFrame, PingWebSocketFrame, PongWebSocketFrame,
  TextWebSocketFrame, WebSocketFrame, WebSocketServerHandshaker
}

import xitrum.{Log, WebSocketBinary, WebSocketPing, WebSocketPong, WebSocketText}
import xitrum.util.ByteBufUtil

/** See https://github.com/netty/netty/blob/master/example/src/main/java/io/netty/example/http/websocketx/server/WebSocketServerHandler.java */
class WebSocketEventDispatcher(
    handshaker: WebSocketServerHandshaker,
    actorRef:   ActorRef
) extends SimpleChannelInboundHandler[WebSocketFrame] with Log
{
  override def channelRead0(ctx: ChannelHandlerContext, frame: WebSocketFrame) {
    if (frame.isInstanceOf[TextWebSocketFrame]) {
      val text = frame.asInstanceOf[TextWebSocketFrame].text
      actorRef ! WebSocketText(text)
      log.trace("[WS in] text: " + text)
      return
    }

    if (frame.isInstanceOf[BinaryWebSocketFrame]) {
      val bytes = ByteBufUtil.toBytes(frame.asInstanceOf[BinaryWebSocketFrame].content)
      actorRef ! WebSocketBinary(bytes)
      log.trace("[WS in] binary: " + ScalaRunTime.stringOf(bytes))
      return
    }

    if (frame.isInstanceOf[PingWebSocketFrame]) {
      ctx.channel.writeAndFlush(new PongWebSocketFrame(frame.content.retain()))
      actorRef ! WebSocketPing
      log.trace("[WS in] ping")
      log.trace("[WS out] pong")
      return
    }

    if (frame.isInstanceOf[PongWebSocketFrame]) {
      actorRef ! WebSocketPong
      log.trace("[WS in] pong")
      return
    }

    if (frame.isInstanceOf[CloseWebSocketFrame]) {
      handshaker.close(ctx.channel, frame.retain().asInstanceOf[CloseWebSocketFrame]).addListener(ChannelFutureListener.CLOSE)
      log.trace("[WS in] close")
      return
    }
  }
}
