package xitrum.view

import scala.xml.Unparsed
import xitrum.Config

trait JSCollector {
  private val buffer = new StringBuilder

  def jsAddToView(js: Any) {
    buffer.append(js.toString)
    buffer.append(";\n")
  }

  def jsForView =
    <script>{Unparsed(
      "var XITRUM_BASE_URI = '" + Config.baseUri + "';\n" +
      "$(function() {\n" + buffer.toString + "});"
    )}</script>
}
