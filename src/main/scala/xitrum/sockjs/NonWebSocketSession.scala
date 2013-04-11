package xitrum.sockjs

import scala.collection.mutable.ArrayBuffer

import akka.actor.{Actor, ActorRef, ActorSystem, Props, ReceiveTimeout, Terminated}
import scala.concurrent.duration._

import xitrum.{Action, SockJsHandler}
import xitrum.routing.Routes

// There are 2 kinds of client: receiver and sender.
// WebSocket is both receiver and sender.
//
// receiver/sender client <-> NonWebSocketSession <-> handler
// (See SockJsAction.scala)                           (See SockJsHandler.scala)

case object SubscribeFromReceiverClient
case object AbortFromReceiverClient

case class  MessagesFromSenderClient(messages: Seq[String])

case class  MessageFromHandler(message: String)
case object CloseFromHandler

case object SubscribeResultToReceiverClientAnotherConnectionStillOpen
case object SubscribeResultToReceiverClientClosed
case class  SubscribeResultToReceiverClientMessages(messages: Seq[String])
case object SubscribeResultToReceiverClientWaitForMessage

case class  NotificationToReceiverClientMessage(message: String)
case object NotificationToReceiverClientHeartbeat
case object NotificationToReceiverClientClosed

object NonWebSocketSession {
  // The session must time out after 5 seconds of not having a receiving connection
  // http://sockjs.github.com/sockjs-protocol/sockjs-protocol-0.3.3.html#section-46
  private val TIMEOUT_CONNECTION = 5.seconds

  private val TIMEOUT_CONNECTION_MILLIS = TIMEOUT_CONNECTION.toMillis

  // The server must send a heartbeat frame every 25 seconds
  // http://sockjs.github.com/sockjs-protocol/sockjs-protocol-0.3.3.html#section-46
  private val TIMEOUT_HEARTBEAT = 25.seconds
}

/**
 * There should be at most one subscriber:
 * http://sockjs.github.com/sockjs-protocol/sockjs-protocol-0.3.3.html
 *
 * To avoid out of memory, the actor is stopped when there's no subscriber
 * for a long time. Timeout is also used to check if there's no message
 * for subscriber for a long time.
 * See TIMEOUT_CONNECTION and TIMEOUT_HEARTBEAT in NonWebSocketSessions.
 */
class NonWebSocketSession(var receiverClient: ActorRef, pathPrefix: String, action: Action) extends Actor {
  import NonWebSocketSession._

  private var sockJsHandler: SockJsHandler = _

  // Messages from handler to client are buffered here
  private val bufferForClientSubscriber = ArrayBuffer[String]()

  // ReceiveTimeout may not occurred if there's frequent Publish, thus we
  // need to manually check if there's no subscriber for a long time.
  // lastSubscribedAt must be Long to avoid Integer overflow, beacuse
  // System.currentTimeMillis() is used.
  private var lastSubscribedAt = 0L

  // Until the timeout occurs, the server must constantly serve
  // the close message
  private var closed = false

  override def preStart() {
    // sockJsHandler.onClose is called at postStop
    sockJsHandler = Routes.createSockJsHandler(pathPrefix)
    sockJsHandler.nonWebSocketSessionActorRef = self
    sockJsHandler.onOpen(action)

    lastSubscribedAt = System.currentTimeMillis()

    context.watch(receiverClient)  // Unsubscribed when stopped

    // Once set, the receive timeout stays in effect (i.e. continues firing
    // repeatedly after inactivity periods). Duration.Undefined must be set
    // to switch off this feature.
    context.setReceiveTimeout(TIMEOUT_CONNECTION)
  }

  override def postStop() {
    sockJsHandler.onClose()

    // Remove interdependency so that JVM can do gabage collection
    sockJsHandler.nonWebSocketSessionActorRef = null
    sockJsHandler = null
  }

  def receive = {
    // When non-WebSocket receiverClient stops normally after sending data to
    // browser, we need to wait for TIMEOUT_CONNECTION amount of time for the
    // to reconnect. Non-streaming client disconnects everytime. Note that for
    // browser to do garbage collection, streaming client also disconnects after
    // sending a large amount of data (4KB in test mode).
    //
    // See also AbortFromReceiverClient below.
    case Terminated(monitored) =>
      if (monitored == receiverClient) {
        receiverClient = null
        context.setReceiveTimeout(TIMEOUT_CONNECTION)
      }

    // Similar to Terminated but no TIMEOUT_CONNECTION is needed
    case AbortFromReceiverClient =>
      context.stop(self)

    case SubscribeFromReceiverClient =>
      if (closed) {
        sender ! SubscribeResultToReceiverClientClosed
      } else {
        lastSubscribedAt = System.currentTimeMillis()
        if (receiverClient == null) {
          receiverClient = sender
          context.watch(receiverClient)  // Unsubscribed when stopped
          if (bufferForClientSubscriber.isEmpty) {
            receiverClient ! SubscribeResultToReceiverClientWaitForMessage
            context.setReceiveTimeout(TIMEOUT_HEARTBEAT)
          } else {
            receiverClient ! SubscribeResultToReceiverClientMessages(bufferForClientSubscriber.toList)
            bufferForClientSubscriber.clear()
          }
        } else {
          sender ! SubscribeResultToReceiverClientAnotherConnectionStillOpen
        }
      }

    case CloseFromHandler =>
      // Until the timeout occurs, the server must serve the close message
      closed = true
      if (receiverClient != null) {
        context.unwatch(receiverClient)
        receiverClient ! NotificationToReceiverClientClosed
        receiverClient = null
      }

    case MessagesFromSenderClient(messages) =>
      if (!closed) messages.foreach(sockJsHandler.onMessage(_))

    case MessageFromHandler(message) =>
      if (!closed) {
        if (receiverClient == null) {
          // Stop to avoid out of memory if there's no subscriber for a long time
          val now = System.currentTimeMillis()
          if (now - lastSubscribedAt > TIMEOUT_CONNECTION_MILLIS) {
            context.stop(self)
          } else {
            bufferForClientSubscriber += message
          }
        } else {
          // buffer is empty at this moment
          receiverClient ! NotificationToReceiverClientMessage(message)
          context.setReceiveTimeout(TIMEOUT_CONNECTION)
        }
      }

    case ReceiveTimeout =>
      if (closed || receiverClient == null) {
        // Closed or no subscriber for a long time
        context.stop(self)
      } else {
        // No message for subscriber for a long time
        receiverClient ! NotificationToReceiverClientHeartbeat
        context.setReceiveTimeout(TIMEOUT_CONNECTION)
      }
  }
}
