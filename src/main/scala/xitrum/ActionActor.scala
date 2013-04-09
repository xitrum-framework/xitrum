package xitrum

import akka.actor.Actor
import xitrum.handler.HandlerEnv

trait ActionActor extends Actor with Action {
  def receive = {
    case (env: HandlerEnv, cacheSecs: Int) =>
      apply(env)
      addConnectionClosedListener { context.stop(self) }
      dispatchWithFailsafe(cacheSecs)
  }

  override def onResponded() {
    context.stop(self)
  }
}
