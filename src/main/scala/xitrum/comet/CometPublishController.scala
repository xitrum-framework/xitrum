package xitrum.comet

import xitrum.Controller

class CometPublishController extends Controller {
  val postback = POSTBACK {
    val channel = param("channel")
    Comet.publish(channel, textParams - "channel")  // Save some space
    renderText("")
  }
}
