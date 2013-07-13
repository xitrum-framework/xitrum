package xitrum

import akka.actor.Actor
import xitrum.handler.HandlerEnv

/**
 * An actor will be created when there's request. It will be stopped when:
 * - The connection is closed
 * - The response has been sent by respondText, respondView etc.
 *
 * For chunked response, it is not stopped right away. It is stopped when the
 * last chunk is sent.
 */
trait ActionActor extends Actor with Action {
  def receive = {
    case env: HandlerEnv =>
      apply(env)

      // Don't use context.stop(self) to avoid leaking context outside this actor
      addConnectionClosedListener { Config.actorSystem.stop(self) }

      dispatchWithFailsafe()
  }

  override def onDoneResponding() {
    // Don't use context.stop(self) to avoid leaking context outside this actor,
    // just in case onDoneResponding is called from another thread
    Config.actorSystem.stop(self)
  }
}
