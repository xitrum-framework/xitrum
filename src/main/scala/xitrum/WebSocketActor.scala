package xitrum

import akka.actor.Actor
import org.jboss.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory

import xitrum.handler.{DefaultHttpChannelPipelineFactory, HandlerEnv}
import xitrum.handler.up.WebSocketEventDispatcher

case class WebSocketText(text: String)

case class WebSocketBinary(bytes: Array[Byte])

/** Pong is automatically sent by Xitrum, don't send it yourself. */
case object WebSocketPing

case object WebSocketPong

/**
 * An actor will be created when there's request. It will be stopped when:
 * - The connection is closed
 * - WebSocket close frame is received or sent
 *
 * You can access request headers, session etc.
 * but do not use respondText, respondView etc.
 * Use these to send WebSocket frames:
 * - respondWebSocketText
 * - respondWebSocketBinary
 * - respondWebSocketPing
 * - respondWebSocketClose
 *
 * There's no respondWebSocketPong, because pong is automatically sent by Xitrum for you.
 */
trait WebSocketActor extends Actor with Action {
  def receive = {
    case env: HandlerEnv =>
      apply(env)

      if (acceptWebSocket()) {
        // Can't use context.stop(self), that means context is leaked outside this actor
        addConnectionClosedListener { Config.actorSystem.stop(self) }

        dispatchWithFailsafe()

        // Resume reading paused at NoPipelining
        channel.setReadable(true)
      } else {
        context.stop(self)
      }
  }

  private def acceptWebSocket(): Boolean = {
    val factory    = new WebSocketServerHandshakerFactory(webSocketAbsRequestUrl, null, false)
    val handshaker = factory.newHandshaker(request)
    if (handshaker == null) {
      factory.sendUnsupportedWebSocketVersionResponse(channel)
      false
    } else {
      handshaker.handshake(channel, request)

      val pipeline = channel.getPipeline
      DefaultHttpChannelPipelineFactory.removeUnusedForWebSocket(pipeline)
      pipeline.addLast("webSocketEventDispatcher", new WebSocketEventDispatcher(handshaker, self))
      true
    }
  }
}
