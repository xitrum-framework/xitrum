package xitrum

import akka.actor.Actor

import xitrum.handler.HandlerEnv

trait Action extends Actor with ActionEnv {
  def receive = {
    case (env: HandlerEnv, cacheSecs: Int) =>
      apply(env)
      addConnectionClosedListener { context.stop(self) }
      dispatchWithFailsafe(cacheSecs)
  }

  def onResponded() {
    context.stop(self)
  }
}
