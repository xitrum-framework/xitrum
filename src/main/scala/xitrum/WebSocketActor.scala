package xitrum

import scala.runtime.ScalaRunTime
import akka.actor.{Actor, PoisonPill}
import io.netty.buffer.{ByteBuf, Unpooled}
import io.netty.channel.{Channel, ChannelFuture, ChannelFutureListener}
import io.netty.handler.codec.http.websocketx.{
  BinaryWebSocketFrame,
  CloseWebSocketFrame,
  PingWebSocketFrame,
  PongWebSocketFrame,
  TextWebSocketFrame,
  WebSocketServerHandshakerFactory
}
import xitrum.handler.{AccessLog, DefaultHttpChannelInitializer, HandlerEnv}
import xitrum.handler.inbound.WebSocketEventDispatcher
import xitrum.handler.DefaultHttpChannelInitializer

//------------------------------------------------------------------------------

case class WebSocketText(text: String)

case class WebSocketBinary(bytes: Array[Byte])

/** Pong is automatically sent by Xitrum, don't send it yourself. */
case object WebSocketPing

case object WebSocketPong

/**
 * An actor will be created when there's request. It will be stopped when:
 * - The connection is closed
 * - WebSocket close frame is received or sent
 */
trait WebSocketActor extends Actor with Action {
  def receive = {
    case env: HandlerEnv =>
      val beginTimestamp = System.currentTimeMillis()
      apply(env)

      if (acceptWebSocket()) {
        // Don't use context.stop(self) to avoid leaking context outside this actor
        addConnectionClosedListener { self ! PoisonPill }

        // Can't use dispatchWithFailsafe because it may respond normal HTTP
        // response; we have just upgraded the connection to WebSocket protocol
        // at acceptWebSocket
        execute()
        AccessLog.logWebSocketAccess(getClass.getName, this, beginTimestamp)
      } else {
        context.stop(self)
      }
  }

  /**
   * You can extract session data, request headers etc. from the current action,
   * but do not use respondText, respondView etc. Use these to send WebSocket frames:
   * - respondWebSocketText
   * - respondWebSocketBinary
   * - respondWebSocketPing
   * - respondWebSocketClose
   *
   * There's no respondWebSocketPong, because Xitrum automatically sends pong for you.
   */
  def execute()

  //----------------------------------------------------------------------------

  def respondWebSocketText(text: Any): ChannelFuture = {
    if (log.isTraceEnabled) log.trace("[WS out] text: " + text)
    channel.write(new TextWebSocketFrame(text.toString))
  }

  def respondWebSocketBinary(bytes: Array[Byte]): ChannelFuture = {
    if (log.isTraceEnabled) log.trace("[WS out] binary: " + ScalaRunTime.stringOf(bytes))
    channel.write(new BinaryWebSocketFrame(Unpooled.wrappedBuffer(bytes)))
  }

  def respondWebSocketBinary(byteBuf: ByteBuf): ChannelFuture = {
    channel.write(new BinaryWebSocketFrame(byteBuf))
  }

  /** There's no respondWebSocketPong, because pong is automatically sent by Xitrum for you. */
  def respondWebSocketPing(): ChannelFuture = {
    if (log.isTraceEnabled) log.trace("[WS out] ping")
    channel.write(new PingWebSocketFrame)
  }

  /** Connection is automatically closed. */
  def respondWebSocketClose(): ChannelFuture = {
    val future = channel.write(new CloseWebSocketFrame)
    future.addListener(ChannelFutureListener.CLOSE)
    if (log.isTraceEnabled) log.trace("[WS out] close")
    future
  }

  //----------------------------------------------------------------------------

  private def acceptWebSocket(): Boolean = {
    val factory    = new WebSocketServerHandshakerFactory(webSocketAbsRequestUrl, null, false)
    val handshaker = factory.newHandshaker(request)
    if (handshaker == null) {
      WebSocketServerHandshakerFactory.sendUnsupportedWebSocketVersionResponse(channel)
      channel.flush().close()
      false
    } else {
      handshaker.handshake(channel, request)

      val pipeline = channel.pipeline
      DefaultHttpChannelInitializer.removeUnusedHandlersForWebSocket(pipeline)
      pipeline.addLast("webSocketEventDispatcher", new WebSocketEventDispatcher(handshaker, self))

      // Resume reading paused at NoPipelining
      channel.config.setAutoRead(true)

      true
    }
  }
}
