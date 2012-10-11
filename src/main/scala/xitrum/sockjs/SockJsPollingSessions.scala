package xitrum.sockjs

import akka.actor.{Actor, ActorRef, ActorSystem, Props, ReceiveTimeout}
import akka.pattern.ask
import akka.util.duration._

import xitrum.Controller
import xitrum.routing.Routes

/** This acts the middleman between client and server SockJS handler. */
object SockJsPollingSessions {
  private val system = ActorSystem("SockJsPollingSessions")

  /**
   * callback:
   * - arg: None means the session has just been openned
   */
  def subscribeOnceByClient(pathPrefix: String, sockJsSessionId: String, callback: (SockJsSubscribeByClientResult) => Unit) {
    val ref = system.actorFor("/user/" + sockJsSessionId)
    if (ref.isTerminated) {
      val handler = Routes.createSockJsHandler(pathPrefix)
      val ref     = system.actorOf(Props(new SockJsPollingSession(handler)), sockJsSessionId)
      handler.sockJsPollingSessionActorRef = ref
      callback(SubscribeByClientResultOpen)
    } else {
      val future = ref.ask(SubscribeOnceByClient)(25 seconds).mapTo[SockJsSubscribeByClientResult]
      future.onComplete {
        case Left(e) =>
          callback(SubscribeByClientResultMessages(Nil))
        case Right(result) =>
          callback(result)
      }
    }
  }

  /**
   * callback:
   * - arg: None means the session has just been openned
   * - result: true means subscribeStreaming should be called again to get more messages
   */
  def subscribeStreamingByClient(pathPrefix: String, sockJsSessionId: String, callback: (SockJsSubscribeByClientResult) => Boolean) {
    val ref = system.actorFor("/user/" + sockJsSessionId)
    if (ref.isTerminated) {
      val handler = Routes.createSockJsHandler(pathPrefix)
      val ref     = system.actorOf(Props(new SockJsPollingSession(handler)), sockJsSessionId)
      handler.sockJsPollingSessionActorRef = ref
      if (callback(SubscribeByClientResultOpen))
        subscribeStreamingByClient(ref, callback)
    } else {
      subscribeStreamingByClient(ref, callback)
    }
  }

  // Called by subscribeStreaming above, but uses ref to avoid actor lookup cost. */
  private def subscribeStreamingByClient(actorRef: ActorRef, callback: (SockJsSubscribeByClientResult) => Boolean) {
    val future = actorRef.ask(SubscribeOnceByClient)(25 seconds).mapTo[SockJsSubscribeByClientResult]
    future.onComplete {
      case Left(e) =>
        if (callback(SubscribeByClientResultMessages(Nil)))
          subscribeStreamingByClient(actorRef, callback)
      case Right(result) =>
        if (callback(result) && result != SubscribeByClientResultAnotherConnectionStillOpen)
          subscribeStreamingByClient(actorRef, callback)
    }
  }

  /** @return false means session not found */
  def sendMessagesByClient(sockJsSessionId: String, messages: Seq[String]): Boolean = {
    val ref = system.actorFor("/user/" + sockJsSessionId)
    if (ref.isTerminated) {
      false
    } else {
      ref ! SendMessagesByClient(messages)
      true
    }
  }

  def unsubscribeByClient(sockJsSessionId: String) {
    val ref = system.actorFor("/user/" + sockJsSessionId)
    ref ! UnsubscribeByClient
  }

  def closeByClient(sockJsSessionId: String) {
    val ref = system.actorFor("/user/" + sockJsSessionId)
    system.stop(ref)
  }

  //----------------------------------------------------------------------------

  def sendMessagesByHandler(actorRef: ActorRef, messages: Seq[String]): Boolean = {
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
}
