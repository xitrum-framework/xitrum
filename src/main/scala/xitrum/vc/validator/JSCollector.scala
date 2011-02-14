package xitrum.vc.validator

import scala.xml.Unparsed

trait JSCollector {
  private val buffer = new StringBuilder

  def jsAddToView(js: String) {
    buffer.append(js)
    buffer.append("\n")
  }

  def jsForView =
    <script>{Unparsed(
      "$(function() {\n" +
        buffer.toString +
      "})"
    )}</script>
}
