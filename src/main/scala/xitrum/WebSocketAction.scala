package xitrum

import scala.runtime.ScalaRunTime
import akka.actor.{Actor, PoisonPill}

import io.netty.buffer.{ByteBuf, Unpooled}
import io.netty.channel.{ChannelFuture, ChannelFutureListener}
import io.netty.handler.codec.http.websocketx.{
  BinaryWebSocketFrame,
  CloseWebSocketFrame,
  PingWebSocketFrame,
  TextWebSocketFrame,
  WebSocketServerHandshakerFactory
}

import xitrum.handler.{AccessLog, HandlerEnv, NoRealPipelining}
import xitrum.handler.inbound.{BadClientSilencer, WebSocketEventDispatcher}
import xitrum.handler.DefaultHttpChannelInitializer
import xitrum.util.SeriDeseri

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
trait WebSocketAction extends Actor with Action {
  def receive = {
    case (env: HandlerEnv, skipCsrfCheck: Boolean) =>
      // env must be set before calling acceptWebSocket, because acceptWebSocket
      // uses webSocketAbsRequestUrl
      apply(env)
      if (acceptWebSocket()) {
        // This only releases native memory. Request headers do not use native
        // memory, thus the "execute" below can still access headers.
        // WebSocket requests are GET, thus don't use native memory, we release
        // just in case.
        env.release()

        // Don't use context.stop(self) to avoid leaking context outside this actor
        addConnectionClosedListener { self ! PoisonPill }

        // Can't use dispatchWithFailsafe because it may respond normal HTTP
        // response; we have just upgraded the connection to WebSocket protocol
        // at acceptWebSocket
        val beginTimestamp = System.currentTimeMillis()
        execute()
        AccessLog.logWebSocketAccess(this, beginTimestamp)
      } else {
        env.release()
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
    log.trace("[WS out] text: " + text)
    channel.writeAndFlush(new TextWebSocketFrame(text.toString))
  }

  def respondWebSocketJson(scalaObject: AnyRef) {
    val json = SeriDeseri.toJson(scalaObject)
    log.trace("[WS out] text: " + json)
    channel.writeAndFlush(new TextWebSocketFrame(json))
  }

  def respondWebSocketBinary(bytes: Array[Byte]): ChannelFuture = {
    log.trace("[WS out] binary: " + ScalaRunTime.stringOf(bytes))
    channel.writeAndFlush(new BinaryWebSocketFrame(Unpooled.wrappedBuffer(bytes)))
  }

  def respondWebSocketBinary(byteBuf: ByteBuf): ChannelFuture = {
    channel.writeAndFlush(new BinaryWebSocketFrame(byteBuf))
  }

  /** There's no respondWebSocketPong, because pong is automatically sent by Xitrum for you. */
  def respondWebSocketPing(): ChannelFuture = {
    log.trace("[WS out] ping")
    channel.writeAndFlush(new PingWebSocketFrame)
  }

  /** Connection is automatically closed. */
  def respondWebSocketClose(): ChannelFuture = {
    val future = channel.writeAndFlush(new CloseWebSocketFrame)
    future.addListener(ChannelFutureListener.CLOSE)
    log.trace("[WS out] close")
    future
  }

  //----------------------------------------------------------------------------

  private def acceptWebSocket(): Boolean = {
    val factory    = new WebSocketServerHandshakerFactory(webSocketAbsRequestUrl, null, false)
    val handshaker = factory.newHandshaker(request)
    if (handshaker == null) {
      val future = WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(channel)
      future.addListener(ChannelFutureListener.CLOSE)
      false
    } else {
      handshaker.handshake(channel, request)

      val pipeline = channel.pipeline
      DefaultHttpChannelInitializer.removeUnusedHandlersForWebSocket(pipeline)
      pipeline.addBefore(
        classOf[BadClientSilencer].getName,
        classOf[WebSocketEventDispatcher].getName,
        new WebSocketEventDispatcher(handshaker, self)
      )

      // Resume reading paused at NoRealPipelining
      NoRealPipelining.resumeReading(channel)

      true
    }
  }
}
