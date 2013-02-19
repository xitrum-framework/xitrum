package xitrum.sockjs

import scala.collection.mutable.ArrayBuffer

import akka.actor.{Actor, ActorRef, ActorSystem, Props, ReceiveTimeout, Terminated}
import scala.concurrent.duration._

import xitrum.{Controller, SockJsHandler}
import xitrum.routing.Routes

sealed trait MessageToSession
case object SubscribeByClient                            extends MessageToSession
case class  SendMessagesByClient(messages: List[String]) extends MessageToSession
case class  SendMessageByHandler(message: String)        extends MessageToSession
case object CloseByHandler                               extends MessageToSession

sealed trait SubscribeResultToClient
case object SubscribeResultToClientAnotherConnectionStillOpen       extends SubscribeResultToClient
case object SubscribeResultToClientClosed                           extends SubscribeResultToClient
case class  SubscribeResultToClientMessages(messages: List[String]) extends SubscribeResultToClient
case object SubscribeResultToClientWaitForMessage                   extends SubscribeResultToClient

sealed trait NotificationToClient
case class  NotificationToClientMessage(message: String) extends NotificationToClient
case object NotificationToClientHeartbeat                extends NotificationToClient
case object NotificationToClientClosed                   extends NotificationToClient

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
class NonWebSocketSession(var clientSubscriber: ActorRef, pathPrefix: String, controller: Controller) extends Actor {
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
    sockJsHandler.onOpen(controller)

    lastSubscribedAt = System.currentTimeMillis()

    context.watch(clientSubscriber)  // Unsubscribed when stopped

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
    case Terminated(monitored) =>
      if (monitored == clientSubscriber) {
        clientSubscriber = null
        context.setReceiveTimeout(TIMEOUT_CONNECTION)
      }

    case SubscribeByClient =>
      if (closed) {
        sender ! SubscribeResultToClientClosed
      } else {
        lastSubscribedAt = System.currentTimeMillis()
        if (clientSubscriber == null) {
          clientSubscriber = sender
          context.watch(clientSubscriber)  // Unsubscribed when stopped
          if (bufferForClientSubscriber.isEmpty) {
            clientSubscriber ! SubscribeResultToClientWaitForMessage
            context.setReceiveTimeout(TIMEOUT_HEARTBEAT)
          } else {
            clientSubscriber ! SubscribeResultToClientMessages(bufferForClientSubscriber.toList)
            bufferForClientSubscriber.clear()
          }
        } else {
          sender ! SubscribeResultToClientAnotherConnectionStillOpen
        }
      }

    case CloseByHandler =>
      // Until the timeout occurs, the server must serve the close message
      closed = true
      if (clientSubscriber != null) {
        context.unwatch(clientSubscriber)
        clientSubscriber ! NotificationToClientClosed
        clientSubscriber = null
      }

    case SendMessagesByClient(messages) =>
      if (!closed) messages.foreach(sockJsHandler.onMessage(_))

    case SendMessageByHandler(message) =>
      if (!closed) {
        if (clientSubscriber == null) {
          // Stop to avoid out of memory if there's no subscriber for a long time
          val now = System.currentTimeMillis()
          if (now - lastSubscribedAt > TIMEOUT_CONNECTION_MILLIS) {
            context.stop(self)
          } else {
            bufferForClientSubscriber += message
          }
        } else {
          // buffer is empty at this moment
          clientSubscriber ! NotificationToClientMessage(message)
          context.setReceiveTimeout(TIMEOUT_CONNECTION)
        }
      }

    case ReceiveTimeout =>
      if (closed || clientSubscriber == null) {
        // Closed or no subscriber for a long time
        context.stop(self)
      } else {
        // No message for subscriber for a long time
        clientSubscriber ! NotificationToClientHeartbeat
        context.setReceiveTimeout(TIMEOUT_CONNECTION)
      }
  }
}
