package xitrum

import akka.actor.{Actor, ActorRef}

import xitrum.handler.{DefaultHttpChannelPipelineFactory, HandlerEnv}
import xitrum.sockjs.{CloseFromHandler, MessageFromHandler}

//------------------------------------------------------------------------------

case class SockJsText(text: String)

/**
 * An actor will be created when there's new SockJS session. It will be stopped when
 * the session is closed.
 */
trait SockJsActor extends Actor {
  // Ref of NonWebSocketSessionActor, SockJSWebsocket, or SockJSRawWebsocket
  private var sessionActorRef: ActorRef = _

  def receive = {
    case (sessionActorRef: ActorRef, action: Action) =>
      this.sessionActorRef = sessionActorRef
      execute(action)
  }

  /**
   * @param action The action just before switching to this SockJS actor.
   * You can extract session data, request headers etc. from it, but do not use
   * respondText, respondView etc. Use respondSockJsText or respondSockJsClose.
   */
  def execute(action: Action)

  //----------------------------------------------------------------------------

  def respondSockJsText(text: String) {
    sessionActorRef ! MessageFromHandler(text)
  }

  def respondSockJsClose() {
    // For non-WebSocket session, until the timeout occurs, the server must serve the close message
    sessionActorRef ! CloseFromHandler
  }
}
