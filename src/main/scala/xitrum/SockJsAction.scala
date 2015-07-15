package xitrum

import scala.collection.mutable.{Map => MMap}
import scala.concurrent.{Future, Promise}
import akka.actor.{Actor, ActorRef}
import xitrum.sockjs.{
  NotificationToHandlerChannelCloseSuccess,
  NotificationToHandlerChannelCloseFailure,
  NotificationToHandlerChannelWriteSuccess,
  CloseFromHandler,
  MessageFromHandler
}
import xitrum.util.SeriDeseri
import xitrum.sockjs.NotificationToHandlerChannelWriteFailure

//------------------------------------------------------------------------------

case class SockJsText(text: String)

/**
 * An actor will be created when there's new SockJS session. It will be stopped when
 * the session is closed.
 */
trait SockJsAction extends Actor with Action {
  // Ref of xitrum.sockjs.{NonWebSocketSessionActor, WebSocket, or RawWebSocket}
  private[this] var sessionActorRef: ActorRef = _
  private[this] var promiseIndex: Int = 0
  private[this] val promises: MMap[Int, Promise[Unit]] = MMap.empty

  private def nextIndex: Int = synchronized {
    promiseIndex += 1
    promiseIndex
  }

  private def createPromise(index: Int): Future[Unit] = {
    val p = Promise[Unit]()
    promises(index) = p
    p.future
  }

  def receive = {
    case (sessionActorRef: ActorRef, action: Action) =>
      this.sessionActorRef = sessionActorRef
      apply(action.handlerEnv)
      execute()

    case NotificationToHandlerChannelCloseSuccess(index) =>
      promises.remove(index).foreach(_.success(Unit))

    case NotificationToHandlerChannelCloseFailure(index) =>
      promises.remove(index).foreach(_.failure(new Throwable))

    case NotificationToHandlerChannelWriteSuccess(index) =>
      promises.remove(index).foreach(_.success(Unit))

    case NotificationToHandlerChannelWriteFailure(index) =>
      promises.remove(index).foreach(_.failure(new Throwable))
  }

  /**
   * The current action is the one just before switching to this SockJS actor.
   * You can extract session data, request headers etc. from it, but do not use
   * respondText, respondView etc. Use respondSockJsText and respondSockJsClose.
   */
  def execute()

  //----------------------------------------------------------------------------

  def respondSockJsText(text: String): Future[Unit] = {
    val index = nextIndex
    sessionActorRef ! MessageFromHandler(index, text)
    createPromise(index)
  }

  def respondSockJsJson(scalaObject: AnyRef): Future[Unit] = {
    val json  = SeriDeseri.toJson(scalaObject)
    respondSockJsText(json)
  }

  def respondSockJsClose(): Future[Unit] = {
    val index = nextIndex
    // sessionActorRef will stop this actor when it stops.
    //
    // For non-WebSocket session, until the timeout occurs, the server must serve
    // the close message.
    sessionActorRef ! CloseFromHandler(index)
    createPromise(index)
  }

  override def postStop() {
    promises.values.foreach(_.failure(new Throwable))
    super.postStop()
  }
}
