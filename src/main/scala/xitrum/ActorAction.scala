package xitrum

import akka.actor.{Actor, PoisonPill}
import xitrum.handler.HandlerEnv

/**
 * An actor will be created when there's request. It will be stopped when:
 * - The connection is closed
 * - The response has been sent by respondText, respondView etc.
 *
 * For chunked response, it is not stopped right away. It is stopped when the
 * last chunk is sent.
 */
trait ActorAction extends Actor with Action {
  def receive = {
    case env: HandlerEnv =>
      apply(env)

      // Don't use context.stop(self) to avoid leaking context outside this actor
      addConnectionClosedListener {
        // The check is for avoiding "Dead actor sends Terminate msg to itself"
        // See onDoneResponding below
        // https://github.com/ngocdaothanh/xitrum/issues/183
        if (!isDoneResponding) self ! PoisonPill
      }

      dispatchWithFailsafe()
  }

  override def onDoneResponding() {
    // Don't use context.stop(self) to avoid leaking context outside this actor,
    // just in case onDoneResponding is called from another thread
    self ! PoisonPill
  }
}
