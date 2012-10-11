package xitrum.sockjs

import scala.collection.mutable.ArrayBuffer

import akka.actor.{Actor, ActorRef, ActorSystem, Props, ReceiveTimeout}
import akka.pattern.ask
import akka.util.duration._

import xitrum.Controller

sealed trait SockJsPollingSessionActorMessage
case class  SendMessagesByClient (messages: Seq[String]) extends SockJsPollingSessionActorMessage
case class  SendMessagesByHandler(messages: Seq[String]) extends SockJsPollingSessionActorMessage
case object SubscribeOnceByClient                        extends SockJsPollingSessionActorMessage
case object UnsubscribeByClient                          extends SockJsPollingSessionActorMessage

sealed trait SockJsSubscribeOnceByClientResult
case class  SubscribeOnceByClientResultMessages(messages: List[String]) extends SockJsSubscribeOnceByClientResult
case object SubscribeOnceByClientResultAnotherConnectionStillOpen       extends SockJsSubscribeOnceByClientResult

/**
 * There should be at most one subscriber:
 * http://sockjs.github.com/sockjs-protocol/sockjs-protocol-0.3.3.html
 */
class SockJsPollingSession(sockJsHandler: SockJsHandler) extends Actor {
  private val TIMEOUT = 25  // Seconds

  private val buffer = ArrayBuffer[String]()
  private var clientSender: ActorRef = null

  // To avoid out of memory, the actor is stopped when there's no subscriber
  // for a long time. This timeout is also used to check if there's no message
  // for subscriber for a long time.
  context.setReceiveTimeout(TIMEOUT seconds)

  // ReceiveTimeout may not occurred if there's frequent Publish, thus we
  // need to manually check if there's no subscriber for a long time
  private var lastSubscribedAt = 0L

  override def preStart() {
    lastSubscribedAt = System.currentTimeMillis()
    sockJsHandler.onOpen()
  }

  def receive = {
    case SendMessagesByClient(messages) =>
      messages.foreach(sockJsHandler.onMessage(_))

    case SendMessagesByHandler(messages) =>
      if (clientSender == null) {
        // Manually check if there's no subscriber for a long time
        val now = System.currentTimeMillis()
        if (now - lastSubscribedAt > TIMEOUT * 1000) {
          context.stop(self)
        } else {
          buffer ++= messages
        }
      } else {
        // buffer is empty at this moment
        clientSender ! SubscribeOnceByClientResultMessages(messages.toList)
        clientSender = null
      }

    case SubscribeOnceByClient =>
      lastSubscribedAt = System.currentTimeMillis()
      if (clientSender == null) {
        if (buffer.isEmpty) {
          clientSender = sender
        } else {
          sender ! SubscribeOnceByClientResultMessages(buffer.toList)
          buffer.clear()
        }
      } else {
        sender ! SubscribeOnceByClientResultAnotherConnectionStillOpen
      }

    case UnsubscribeByClient =>
      clientSender = null

    case ReceiveTimeout =>
      if (clientSender == null) {
        // No subscriber for a long time
        context.stop(self)
      } else {
        // No message for subscriber for a long time
        clientSender ! SubscribeOnceByClientResultMessages(Nil)
        clientSender = null
      }
  }

  override def postStop() {
    sockJsHandler.onClose()
    // Remove interdependency
    sockJsHandler.sockJsPollingSessionActorRef = null
  }
}
