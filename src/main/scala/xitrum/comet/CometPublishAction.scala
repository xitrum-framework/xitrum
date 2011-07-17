package xitrum.comet

import xitrum.Action
import xitrum.annotation.GET

class CometPublishAction extends Action {
  override def execute {
    val channel = param("channel")
    Comet.publish(channel, textParams)
    renderText("")
  }
}
