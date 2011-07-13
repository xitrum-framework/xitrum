package xitrum.comet

import xitrum.Action
import xitrum.annotation.GET

class CometAction extends Action {
  override def execute {
    val channel = param("channel")
    val body    = param("body")
    Comet.publish(channel, body)
    renderText("")
  }
}
