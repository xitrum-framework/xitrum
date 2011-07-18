package xitrum.view

import scala.xml.Unparsed

import xitrum.{Action, Config}
import xitrum.comet.CometGetAction

trait JSCollector {
  this: Action =>

  private val buffer = new StringBuilder

  def jsAddToView(js: Any) {
    buffer.append(js.toString)
    buffer.append(";\n")
  }

  def jsCometGet(channel: String, callback: String) {
    val encryptedChannel       = validate("channel")
    val encryptedLastTimestamp = validate("lastTimestamp")
    jsAddToView("xitrum.cometGet('" + encryptedChannel + "', '" + channel + "', '" + encryptedLastTimestamp + "', 0, " + callback + ")")
  }

  def jsForView =
    <script>{Unparsed(
      "var XITRUM_BASE_URI = '" + Config.baseUri + "';\n" +
      "var XITRUM_COMET_GET_ACTION = '" + urlForPostback[CometGetAction] + "';\n" +
      "$(function() {\n" + buffer.toString + "});"
    )}</script>
}
