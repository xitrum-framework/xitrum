package xitrum

import akka.actor.ActorRef
import com.codahale.jerkson.Json

import xitrum.routing.Routes
import xitrum.sockjs.SockJsPollingSessions

abstract class SockJsHandler extends Logger {
  /** Set by SockJsController; true if raw WebSocket transport is used */
  var rawWebSocket = false

  /** Set by SockJsController; null if WebSocket (raw or not) is used (polling is not used) */
  var sockJsPollingSessionActorRef: ActorRef = null

  /** Set by SockJsController; null if WebSocket (raw or not) is not used (polling is used) */
  var webSocketController: Controller = null

  //----------------------------------------------------------------------------
  // Abstract methods that must be implemented by apps

  def onOpen()
  def onMessage(message: String)
  def onClose()

  //----------------------------------------------------------------------------
  // Helper methods for apps to use

  def send(message: Any) {
    if (webSocketController == null) {
      // FIXME: Ugly code
      // sockJsPollingSessionActorRef is set to null by SockJsPollingSession on postStop
      if (sockJsPollingSessionActorRef != null) {
        if (!SockJsPollingSessions.sendMessageByHandler(sockJsPollingSessionActorRef, message.toString))
          onClose()
      }
    } else {
      // WebSocket is used, but it may be raw or not raw
      if (rawWebSocket) {
        webSocketController.respondWebSocket(message)
      } else {
        webSocketController.respondWebSocket("a" + Json.generate(List(message)) + "\n")
      }
    }
  }

  def close() {
    if (webSocketController == null) {
      // Until the timeout occurs, the server must serve the close message
      SockJsPollingSessions.closeByHandler(sockJsPollingSessionActorRef)
    } else {
      // For WebSocket, must explicitly close
      webSocketController.channel.close()
    }
  }

  def url: String = {
    Config.withBaseUrl(Routes.sockJsPathPrefix(this.getClass))
  }
}
