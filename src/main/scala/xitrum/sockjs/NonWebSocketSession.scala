package xitrum.sockjs

import scala.collection.mutable.ArrayBuffer

import akka.actor.{Actor, ActorRef, ActorSystem, Props, ReceiveTimeout}
import scala.concurrent.duration._

import xitrum.SockJsHandler

sealed trait SockJsNonWebSocketSessionActorMessage
case class  SendMessagesByClient (messages: List[String]) extends SockJsNonWebSocketSessionActorMessage
case class  SendMessageByHandler(message: String)         extends SockJsNonWebSocketSessionActorMessage
case object SubscribeOnceByClient                         extends SockJsNonWebSocketSessionActorMessage
case object UnsubscribeByClient                           extends SockJsNonWebSocketSessionActorMessage
case object CloseByHandler                                extends SockJsNonWebSocketSessionActorMessage

sealed trait SockJsSubscribeByClientResult
case object SubscribeByClientResultOpen                             extends SockJsSubscribeByClientResult
//case object SubscribeByClientResultWaitForMessages                  extends SockJsSubscribeByClientResult
case object SubscribeByClientResultAnotherConnectionStillOpen       extends SockJsSubscribeByClientResult
case object SubscribeByClientResultClosed                           extends SockJsSubscribeByClientResult
case class  SubscribeByClientResultMessages(messages: List[String]) extends SockJsSubscribeByClientResult
case object SubscribeByClientResultErrorAfterOpenHasBeenSent        extends SockJsSubscribeByClientResult

/**
 * There should be at most one subscriber:
 * http://sockjs.github.com/sockjs-protocol/sockjs-protocol-0.3.3.html
 *
 * To avoid out of memory, the actor is stopped when there's no subscriber
 * for a long time. Timeout is also used to check if there's no message
 * for subscriber for a long time.
 * See TIMEOUT_CONNECTION and TIMEOUT_HEARTBEAT in NonWebSocketSessions.
 */
class NonWebSocketSession(sockJsHandler: SockJsHandler) extends Actor {
  private val TIMEOUT_CONNECTION_MILLIS = NonWebSocketSessions.TIMEOUT_CONNECTION.toMillis

  private val buffer = ArrayBuffer[String]()
  private var clientSender: ActorRef = null

  // ReceiveTimeout may not occurred if there's frequent Publish, thus we
  // need to manually check if there's no subscriber for a long time.
  // lastSubscribedAt must be Long to avoid Integer overflow, beacuse
  // System.currentTimeMillis() is used.
  private var lastSubscribedAt = 0L

  // Until the timeout occurs, the server must constantly serve
  // the close message
  private var closed = false

  context.setReceiveTimeout(NonWebSocketSessions.TIMEOUT_CONNECTION)

  override def preStart() {
    // sockJsHandler.onClose is called at postStop, but sockJsHandler.onOpen
    // is not called here, because sockJsHandler.onOpen may send messages, but
    // "o" frame needs to be sent before all messages. sockJsHandler.onOpen will
    // be called by NonWebSocketSessions.
    lastSubscribedAt = System.currentTimeMillis()
  }

  override def postStop() {
    sockJsHandler.onClose()
    // Remove interdependency
    sockJsHandler.nonWebSocketSessionActorRef = null
  }

  def receive = {
    case SubscribeOnceByClient =>
      if (closed) {
        sender ! SubscribeByClientResultClosed
      } else {
        lastSubscribedAt = System.currentTimeMillis()
        if (clientSender == null) {
          if (buffer.isEmpty) {
            clientSender = sender
            context.setReceiveTimeout(NonWebSocketSessions.TIMEOUT_HEARTBEAT)
            //clientSender ! SubscribeByClientResultWaitForMessages
          } else {
            sender ! SubscribeByClientResultMessages(buffer.toList)
            buffer.clear()
          }
        } else {
          sender ! SubscribeByClientResultAnotherConnectionStillOpen
        }
      }

    case UnsubscribeByClient =>
      clientSender = null
      context.setReceiveTimeout(NonWebSocketSessions.TIMEOUT_CONNECTION)

    case CloseByHandler =>
      // Until the timeout occurs, the server must serve the close message
      closed = true

    case SendMessagesByClient(messages) =>
      if (!closed) messages.foreach(sockJsHandler.onMessage(_))

    case SendMessageByHandler(message) =>
      if (!closed) {
        if (clientSender == null) {
          // Manually check if there's no subscriber for a long time
          val now = System.currentTimeMillis()
          if (now - lastSubscribedAt > TIMEOUT_CONNECTION_MILLIS) {
            context.stop(self)
          } else {
            buffer += message
          }
        } else {
          // buffer is empty at this moment
          clientSender ! SubscribeByClientResultMessages(List(message))
          clientSender = null
          context.setReceiveTimeout(NonWebSocketSessions.TIMEOUT_CONNECTION)
        }
      }

    case ReceiveTimeout =>
      if (closed || clientSender == null) {
        // Closed or no subscriber for a long time
        context.stop(self)
      } else {
        // No message for subscriber for a long time
        clientSender ! SubscribeByClientResultMessages(Nil)
        clientSender = null
        context.setReceiveTimeout(NonWebSocketSessions.TIMEOUT_CONNECTION)
      }
  }
}
