package xitrum

import akka.actor.ActorRef
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

  def onOpen(controller: Controller)
  def onMessage(message: String)
  def onClose()

  //----------------------------------------------------------------------------
  // Helper methods for apps to use

  def sendMessage(message: String) {
    sendMessages(List(message))
  }

  def sendMessages(messages: List[String]) {
    if (webSocketController == null) {
      // FIXME: Ugly code
      // sockJsPollingSessionActorRef is set to null by SockJsPollingSession on postStop
      if (sockJsPollingSessionActorRef != null) {
        if (!SockJsPollingSessions.sendMessagesByHandler(sockJsPollingSessionActorRef, messages))
          onClose()
      }
    } else {
      // WebSocket is used, but it may be raw or not raw
      if (rawWebSocket) {
        for (message <- messages)
          webSocketController.respondWebSocket(message)
      } else {
        val json = messages.map(webSocketController.jsEscape(_)).mkString("a[", ",", "]\n")
        webSocketController.respondWebSocket(json)
      }
    }
  }

  def close() {
    if (webSocketController == null) {
      SockJsPollingSessions.closeByHandler(sockJsPollingSessionActorRef)
    } else {
      webSocketController.channel.close()
    }
  }
}
