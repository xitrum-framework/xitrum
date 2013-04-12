package xitrum

import akka.actor.Actor

import org.jboss.netty.buffer.{ChannelBuffer, ChannelBuffers}
import org.jboss.netty.channel.{Channel, ChannelFuture, ChannelFutureListener}
import org.jboss.netty.handler.codec.http.websocketx.{
  BinaryWebSocketFrame,
  CloseWebSocketFrame,
  PingWebSocketFrame,
  PongWebSocketFrame,
  TextWebSocketFrame,
  WebSocketServerHandshakerFactory
}

import xitrum.handler.{AccessLog, DefaultHttpChannelPipelineFactory, HandlerEnv}
import xitrum.handler.up.WebSocketEventDispatcher

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
trait WebSocketActor extends Actor {
  private var channel: Channel = _

  def receive = {
    case env: HandlerEnv =>
      val beginTimestamp = System.currentTimeMillis()

      val action = new Action { def execute() {} }
      action.apply(env)

      if (acceptWebSocket(action)) {
        channel = env.channel

        // Can't use context.stop(self), that means context is leaked outside this actor
        action.addConnectionClosedListener { Config.actorSystem.stop(self) }

        execute(action)
        AccessLog.logWebSocketAccess(getClass.getName, action, beginTimestamp)

        // Resume reading paused at NoPipelining
        channel.setReadable(true)
      } else {
        context.stop(self)
      }
  }

  /**
   * @param action The action just before switching to this WebSocket actor.
   * You can extract session data, request headers etc. from it, but do not use
   * respondText, respondView etc. Use these to send WebSocket frames:
   * - respondWebSocketText
   * - respondWebSocketBinary
   * - respondWebSocketPing
   * - respondWebSocketClose
   *
   * There's no respondWebSocketPong, because pong is automatically sent by Xitrum for you.
   */
  def execute(action: Action)

  //----------------------------------------------------------------------------

  def respondWebSocketText(text: Any): ChannelFuture = {
    channel.write(new TextWebSocketFrame(text.toString))
  }

  def respondWebSocketBinary(bytes: Array[Byte]): ChannelFuture = {
    channel.write(new BinaryWebSocketFrame(ChannelBuffers.wrappedBuffer(bytes)))
  }

  def respondWebSocketBinary(channelBuffer: ChannelBuffer): ChannelFuture = {
    channel.write(new BinaryWebSocketFrame(channelBuffer))
  }

  /** There's no respondWebSocketPong, because pong is automatically sent by Xitrum for you. */
  def respondWebSocketPing(): ChannelFuture = {
    channel.write(new PingWebSocketFrame())
  }

  /** Connection is automatically closed. */
  def respondWebSocketClose(): ChannelFuture = {
    val future = channel.write(new CloseWebSocketFrame())
    future.addListener(ChannelFutureListener.CLOSE)
    future
  }

  //----------------------------------------------------------------------------

  private def acceptWebSocket(action: Action): Boolean = {
    val factory    = new WebSocketServerHandshakerFactory(action.webSocketAbsRequestUrl, null, false)
    val handshaker = factory.newHandshaker(action.request)
    if (handshaker == null) {
      factory.sendUnsupportedWebSocketVersionResponse(action.channel)
      false
    } else {
      handshaker.handshake(action.channel, action.request)

      val pipeline = action.channel.getPipeline
      DefaultHttpChannelPipelineFactory.removeUnusedForWebSocket(pipeline)
      pipeline.addLast("webSocketEventDispatcher", new WebSocketEventDispatcher(handshaker, self))
      true
    }
  }
}
