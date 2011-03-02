package xitrum.action.view

import scala.xml.Unparsed

trait JSCollector {
  private val buffer = new StringBuilder

  def jsAddToView(js: Any) {
    buffer.append(js.toString)
    buffer.append(";\n")
  }

  def jsForView =
    <script>{Unparsed(
      "$(function() {\n" +
        buffer.toString +
      "});"
    )}</script>
}
