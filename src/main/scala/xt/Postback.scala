package xt

trait Postback {
  this: Action =>

  def postback

  def renderJS(value: Any) {
    val js = escapeJS(value.toString)
    renderText(js)
  }

  def renderUpdate(id: String, value: Any) {
    val js = "$('#" + id + "').text(\"" + escapeJS(value.toString) + "\")"
    renderText(js)
  }

  /** See escape_javascript of Rails */
  def escapeJS(js: String) = js
    .replace("\\\\", "\\0\\0")
    .replace("</",   "<\\/")
    .replace("\r\n", "\\n")
    .replace("\n",   "\\n")
    .replace("\r",   "\\n")
    .replace("\"",   "\\\"")
    .replace("'",    "\\'")
}
