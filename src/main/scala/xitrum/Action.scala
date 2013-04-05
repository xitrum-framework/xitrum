package xitrum

import akka.actor.Actor
import xitrum.handler.HandlerEnv

trait Action extends Actor with ActionEnv {
  def receive = {
    case env: HandlerEnv =>
      apply(env)
  }
}
