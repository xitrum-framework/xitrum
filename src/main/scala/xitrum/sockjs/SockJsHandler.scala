package xitrum.sockjs

import akka.actor.ActorRef
import xitrum.Controller

abstract class SockJsHandler {
  /** Set by SockJsController; null if WebSocket is used (polling is not used) */
  var sockJsPollingSessionActorRef: ActorRef = null

  /** Set by SockJsController; null if WebSocket is not used (polling is used) */
  var webSocketController: Controller = null

  /** Set by SockJsController; true if raw WebSocket transport is used */
  var rawWebSocket = false

  //----------------------------------------------------------------------------
  // Abstract methods that must be implemented by apps

  def onOpen()
  def onMessage(message: String)
  def onClose()

  //----------------------------------------------------------------------------
  // Helper methods for apps to use

  def sendMessage(message: String) {
    sendMessages(List(message))
  }

  def sendMessages(messages: List[String]) {
    if (webSocketController == null) {
      if (!SockJsPollingSessions.sendMessagesByHandler(sockJsPollingSessionActorRef, messages))
        onClose()
    } else if (rawWebSocket) {
      for (message <- messages)
        webSocketController.respondWebSocket(message)
    } else {
      val json = messages.map(webSocketController.jsEscape(_)).mkString("a[", ",", "]\n")
      webSocketController.respondWebSocket(json)
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
