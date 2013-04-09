package xitrum

import akka.actor.Actor
import xitrum.handler.HandlerEnv

trait ActionActor extends Actor with Action {
  def receive = {
    case env: HandlerEnv =>
      apply(env)

      // Can't use context.stop(self), that means context is leaked outside this actor
      addConnectionClosedListener { Config.actorSystem.stop(self) }

      dispatchWithFailsafe()
  }

  override def onResponded() {
    context.stop(self)
  }
}
