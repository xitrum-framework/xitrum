package xitrum

import akka.actor.Actor
import xitrum.handler.HandlerEnv

trait ActionActor extends Actor with Action {
  def receive = {
    case env: HandlerEnv =>
      apply(env)
      addConnectionClosedListener { context.stop(self) }
      dispatchWithFailsafe()
  }

  override def onResponded() {
    context.stop(self)
  }
}
