package xitrum.handler.inbound

import scala.runtime.ScalaRunTime
import akka.actor.ActorRef

import io.netty.channel.{
  ChannelHandlerContext, ChannelFutureListener, SimpleChannelInboundHandler
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
) extends SimpleChannelInboundHandler[WebSocketFrame]
{
  override def channelRead0(ctx: ChannelHandlerContext, frame: WebSocketFrame) {
    frame match {
      case textFrame: TextWebSocketFrame =>
        val text = textFrame.text
        actorRef ! WebSocketText(text)
        Log.trace("[WS in] text: " + text)

      case binaryFrame: BinaryWebSocketFrame =>
        val bytes = ByteBufUtil.toBytes(binaryFrame.content)
        actorRef ! WebSocketBinary(bytes)
        Log.trace("[WS in] binary: " + ScalaRunTime.stringOf(bytes))

      case pingFrame: PingWebSocketFrame =>
        ctx.channel.writeAndFlush(new PongWebSocketFrame(pingFrame.content.retain()))
        actorRef ! WebSocketPing
        Log.trace("[WS in] ping")
        Log.trace("[WS out] pong")

      case pongFrame: PongWebSocketFrame =>
        actorRef ! WebSocketPong
        Log.trace("[WS in] pong")

      case closeFrame: CloseWebSocketFrame =>
        handshaker.close(ctx.channel, frame.retain().asInstanceOf[CloseWebSocketFrame]).addListener(ChannelFutureListener.CLOSE)
        Log.trace("[WS in] close")

      case otherFrame =>
        Log.trace("[WS in] unprocessed frame: $otherFrame")
    }
  }
}
