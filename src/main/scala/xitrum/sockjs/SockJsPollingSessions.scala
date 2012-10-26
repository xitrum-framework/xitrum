package xitrum.sockjs

import java.net.URLEncoder

import akka.actor.{Actor, ActorRef, ActorSystem, Props, ReceiveTimeout}
import akka.pattern.ask
import akka.util.duration._

import xitrum.Controller
import xitrum.routing.Routes

/** This acts the middleman between client and server SockJS handler. */
object SockJsPollingSessions {
  // Seconds
  // TIMEOUT_FOR_SockJsPollingSessions needs to be bigger than
  // TIMEOUT_FOR_SockJsPollingSession so that timeout for SockJsPollingSessions
  // does not happen before timeout for SockJsPollingSession
  val TIMEOUT_FOR_SockJsPollingSession  = 25
  val TIMEOUT_FOR_SockJsPollingSessions = TIMEOUT_FOR_SockJsPollingSession * 2

  private val system = ActorSystem("SockJsPollingSessions")

  def subscribeOnceByClient(pathPrefix: String, sockJsSessionId: String, callback: (SockJsSubscribeByClientResult) => Unit) {
    val escaped = escapeActorPath(sockJsSessionId)
    val ref     = system.actorFor("/user/" + escaped)
    if (ref.isTerminated) {
      val handler = Routes.createSockJsHandler(pathPrefix)
      val ref     = system.actorOf(Props(new SockJsPollingSession(handler)), escaped)
      handler.sockJsPollingSessionActorRef = ref
      callback(SubscribeByClientResultOpen)
      handler.onOpen()  // Call opOpen after "o" frame has been sent
    } else {
      val future = ref.ask(SubscribeOnceByClient)(TIMEOUT_FOR_SockJsPollingSessions seconds).mapTo[SockJsSubscribeByClientResult]
      future.onComplete {
        case Left(e) =>
          // The channel will be closed by SockJsController,
          // handler.onClose is called at SockJsPollingSession#postStop
          callback(SubscribeByClientResultErrorAfterOpenHasBeenSent)
        case Right(result) =>
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
      val ref     = system.actorOf(Props(new SockJsPollingSession(handler)), escaped)
      handler.sockJsPollingSessionActorRef = ref
      if (callback(SubscribeByClientResultOpen)) subscribeStreamingByClient(ref, callback)
      handler.onOpen()  // Call opOpen after "o" frame has been sent
    } else {
      subscribeStreamingByClient(ref, callback)
    }
  }

  /** Called by subscribeStreaming above, but uses ref to avoid actor lookup cost. */
  private def subscribeStreamingByClient(actorRef: ActorRef, callback: (SockJsSubscribeByClientResult) => Boolean) {
    val future = actorRef.ask(SubscribeOnceByClient)(TIMEOUT_FOR_SockJsPollingSessions seconds).mapTo[SockJsSubscribeByClientResult]
    future.onComplete {
      case Left(e) =>
        // The channel will be closed by SockJsController,
        // handler.onClose is called at SockJsPollingSession#postStop
        callback(SubscribeByClientResultErrorAfterOpenHasBeenSent)
      case Right(result) =>
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

  def closeByClient(sockJsSessionId: String) {
    val escaped = escapeActorPath(sockJsSessionId)
    val ref     = system.actorFor("/user/" + escaped)
    system.stop(ref)
  }

  //----------------------------------------------------------------------------

  def sendMessagesByHandler(actorRef: ActorRef, messages: List[String]): Boolean = {
    if (actorRef.isTerminated) {
      false
    } else {
      actorRef ! SendMessagesByHandler(messages)
      true
    }
  }

  def closeByHandler(actorRef: ActorRef) {
    system.stop(actorRef)
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
