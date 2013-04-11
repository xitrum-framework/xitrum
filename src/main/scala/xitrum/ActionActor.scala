package xitrum

import akka.actor.Actor
import xitrum.handler.HandlerEnv

/**
 * An actor will be created when there's request. It will be stopped when the
 * connection is closed or when the response has been sent by respondText,
 * respondView etc. methods. For chunked response, it is not stopped right away.
 * It is stopped when the last chunk is sent.
 */
trait ActionActor extends Actor with Action {
  def receive = {
    case env: HandlerEnv =>
      apply(env)

      // Can't use context.stop(self), that means context is leaked outside this actor
      addConnectionClosedListener { Config.actorSystem.stop(self) }

      dispatchWithFailsafe()
  }

  override def onDoneResponding() {
    // Avoid using context.stop(self) to avoid leaking actor internals (context)
    // outside this actor, just in case onDoneResponding is called from another thread
    Config.actorSystem.stop(self)
  }
}
