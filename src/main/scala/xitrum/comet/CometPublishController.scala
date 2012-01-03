package xitrum.comet

import xitrum.Controller

object CometPublishController extends CometPublishController

class CometPublishController extends Controller {
  val postback = indirectRoute {
    val channel = param("channel")
    Comet.publish(channel, textParams - "channel")  // Save some space
    renderText("")
  }
}
