package xitrum

import akka.actor.{Actor, PoisonPill}
import io.netty.handler.codec.http.HttpResponseStatus
import xitrum.handler.HandlerEnv
import xitrum.handler.inbound.Dispatcher

/**
 * An actor will be created when there's request. It will be stopped when:
 * - The connection is closed
 * - The response has been sent by respondText, respondView etc.
 *
 * For chunked response, it is not stopped right away. It is stopped when the
 * last chunk is sent.
 *
 * See also Action and FutureAction.
 */
trait ActorAction extends Actor with Action {
  // Sending PoisonPill at postStop causes postStop to be called again!
  private var postStopCalled = false

  def receive = {
    case env: HandlerEnv =>
      apply(env)

      // Don't use context.stop(self) to avoid leaking context outside this actor
      addConnectionClosedListener {
        // The check is for avoiding "Dead actor sends Terminate msg to itself"
        // See onDoneResponding below
        // https://github.com/xitrum-framework/xitrum/issues/183
        if (!isDoneResponding) {
          env.release()
          if (!postStopCalled) self ! PoisonPill
        }
      }

      dispatchWithFailsafe()
  }

  override def onDoneResponding() {
    // Don't use context.stop(self) to avoid leaking context outside this actor,
    // just in case onDoneResponding is called from another thread
    if (!postStopCalled) self ! PoisonPill
  }

  override def postStop() {
    if (postStopCalled) return
    postStopCalled = true

    // When there's uncaught exception (dispatchWithFailsafe can only catch
    // exception thrown from the current calling thread, not from other threads):
    // - This actor is stopped without responding anything
    // - Akka won't pass the exception to this actor, it will just log the error

    if (isDoneResponding) return

    if (!channel.isWritable) {
      handlerEnv.release()
      return
    }

    response.setStatus(HttpResponseStatus.INTERNAL_SERVER_ERROR)
    if (Config.productionMode) {
      Config.routes.error500 match {
        case None =>
          respondDefault500Page()

        case Some(error500) =>
          if (error500 == getClass) {
            respondDefault500Page()
          } else {
            response.setStatus(HttpResponseStatus.INTERNAL_SERVER_ERROR)
            Dispatcher.dispatch(error500, handlerEnv)
          }
      }
    } else {
      val errorMsg = s"The ActorAction ${getClass.getName} has stopped without responding anything. Check server log for exception."
      if (isAjax)
        jsRespond(s"""alert("${jsEscape(errorMsg)}")""")
      else
        respondText(errorMsg)
    }
  }
}
