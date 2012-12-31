package xitrum.sockjs

import java.net.URLEncoder

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

import akka.actor.{Actor, ActorRef, ActorSystem, Props, ReceiveTimeout}
import akka.pattern.ask

import xitrum.Controller
import xitrum.routing.Routes

/** This acts the middleman between client and server SockJS handler. */
object SockJsNonWebSocketSessions {
  // The session must time out after 5 seconds of not having a receiving connection
  // http://sockjs.github.com/sockjs-protocol/sockjs-protocol-0.3.3.html#section-46
  val TIMEOUT_CONNECTION = 5.seconds

  // The server must send a heartbeat frame every 25 seconds
  // http://sockjs.github.com/sockjs-protocol/sockjs-protocol-0.3.3.html#section-46
  val TIMEOUT_HEARTBEAT = 25.seconds

  // Must be bigger than TIMEOUT_HEARTBEAT "ask" timeout does not happen before
  // heartbeat timeout
  val TIMEOUT_ASK = TIMEOUT_HEARTBEAT * 2

  private val system = ActorSystem("SockJsNonWebSocketSessions")

  def subscribeOnceByClient(pathPrefix: String, sockJsSessionId: String, callback: (SockJsSubscribeByClientResult) => Unit) {
    val escaped = escapeActorPath(sockJsSessionId)
    val ref     = system.actorFor("/user/" + escaped)
    if (ref.isTerminated) {
      val handler = Routes.createSockJsHandler(pathPrefix)
      val ref     = system.actorOf(Props(new SockJsNonWebSocketSession(handler)), escaped)
      handler.sockJsNonWebSocketSessionActorRef = ref
      callback(SubscribeByClientResultOpen)
      handler.onOpen()  // Call opOpen after "o" frame has been sent
    } else {
      val future = ref.ask(SubscribeOnceByClient)(TIMEOUT_ASK).mapTo[SockJsSubscribeByClientResult]
      future.onComplete {
        case Failure(e) =>
          // The channel will be closed by SockJsController,
          // handler.onClose is called at SockJsNonWebSocketSession#postStop
          callback(SubscribeByClientResultErrorAfterOpenHasBeenSent)
        case Success(result) =>
          callback(result)
      }
    }
  }

  /**
   * callback result: true means subscribeStreaming should be called again to get more messages
   */
  def subscribeStreamingByClient(pathPrefix: String, sockJsSessionId: String, callback: (SockJsSubscribeByClientResult) => Boolean) {
    val escaped = escapeActorPath(sockJsSessionId)
    val ref     = system.actorFor("/user/" + escaped)
    if (ref.isTerminated) {
      val handler = Routes.createSockJsHandler(pathPrefix)
      val ref     = system.actorOf(Props(new SockJsNonWebSocketSession(handler)), escaped)
      handler.sockJsNonWebSocketSessionActorRef = ref
      val loop = callback(SubscribeByClientResultOpen)  // "o" frame sent here
      handler.onOpen()                                  // Call opOpen after "o" frame has been sent
      if (loop) subscribeStreamingByClient(ref, callback)
    } else {
      subscribeStreamingByClient(ref, callback)
    }
  }

  /** Called by subscribeStreaming above, but uses ref to avoid actor lookup cost. */
  private def subscribeStreamingByClient(actorRef: ActorRef, callback: (SockJsSubscribeByClientResult) => Boolean) {
    val future = actorRef.ask(SubscribeOnceByClient)(TIMEOUT_ASK).mapTo[SockJsSubscribeByClientResult]
    future.onComplete {
      case Failure(e) =>
        // The channel will be closed by SockJsController,
        // handler.onClose is called at SockJsNonWebSocketSession#postStop
        callback(SubscribeByClientResultErrorAfterOpenHasBeenSent)
      case Success(result) =>
        if (callback(result) && result != SubscribeByClientResultAnotherConnectionStillOpen)
          subscribeStreamingByClient(actorRef, callback)
    }
  }

  /** @return false means session not found */
  def sendMessagesByClient(sockJsSessionId: String, messages: List[String]): Boolean = {
    val escaped = escapeActorPath(sockJsSessionId)
    val ref     = system.actorFor("/user/" + escaped)
    if (ref.isTerminated) {
      false
    } else {
      ref ! SendMessagesByClient(messages)
      true
    }
  }

  def unsubscribeByClient(sockJsSessionId: String) {
    val escaped = escapeActorPath(sockJsSessionId)
    val ref     = system.actorFor("/user/" + escaped)
    ref ! UnsubscribeByClient
  }

  /**
   * When SockJsController wants to send keep alive message or normal message,
   * as the result of SubscribeByClientResultMessages, but the connection has
   * been aborted, it will call this method to remove the session.
   * See http://groups.google.com/group/sockjs/browse_thread/thread/392cd07c4a75400b/9a4593a71e90173b#9a4593a71e90173b
   */
  def abortByClient(sockJsSessionId: String) {
    val escaped = escapeActorPath(sockJsSessionId)
    val ref     = system.actorFor("/user/" + escaped)
    system.stop(ref)
  }

  //----------------------------------------------------------------------------

  def sendMessageByHandler(actorRef: ActorRef, message: String): Boolean = {
    if (actorRef.isTerminated) {
      false
    } else {
      actorRef ! SendMessageByHandler(message)
      true
    }
  }

  /** Until the timeout occurs, the server must serve the close message. */
  def closeByHandler(actorRef: ActorRef) {
    actorRef ! CloseByHandler
  }

  //----------------------------------------------------------------------------

  /**
   * Need this because java.net.URISyntaxException will be thrown if
   * sockJsSessionId contains strange charater.
   */
  private def escapeActorPath(sockJsSessionId: String) = {
    URLEncoder.encode(sockJsSessionId, "UTF-8")
  }
}
