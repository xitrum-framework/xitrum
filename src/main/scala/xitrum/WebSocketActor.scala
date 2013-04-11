package xitrum

/**
 * An actor will be created when there's request. It will be stopped when:
 * - The connection is closed
 * - WebSocket close frame is received or sent
 *
 * Use these to send WebSocket frames:
 * - respondWebSocketText
 * - respondWebSocketBinary
 * - respondWebSocketPing
 * - respondWebSocketClose
 *
 * There's no respondWebSocketPong, because pong is automatically sent by Xitrum for you.
 */
trait WebSocketActor extends ActionActor {
  case class WebSocketText(text: String)

  case class WebSocketBinary(bytes: Array[Byte])

  /** Pong is automatically sent by Xitrum, don't send it yourself. */
  case object WebSocketPing

  case object WebSocketPong
}
