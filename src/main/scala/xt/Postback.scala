package xt

import org.jboss.netty.handler.codec.http.HttpHeaders.Names._

object Postback {
  val js =
    <script type="text/javascript" src="/resources/public/xt/jquery-1.5.min.js"></script>
    <script type="text/javascript" src="/resources/public/xt/xitrum.js"></script>
}

trait Postback {
  this: Action =>

  def postback

  /** See escape_javascript of Rails */
  def jsEscape(value: Any) = value.toString
    .replace("\\\\", "\\0\\0")
    .replace("</",   "<\\/")
    .replace("\r\n", "\\n")
    .replace("\n",   "\\n")
    .replace("\r",   "\\n")
    .replace("\"",   "\\\"")
    .replace("'",    "\\'")

  def jsUpdate(id: String, value: Any) =
    "$(\"#" + id + "\").html(\"" + jsEscape(value.toString) + "\")"

  def renderJS(values: String*) {
    val js = values.mkString(";\n")
    response.setHeader(CONTENT_TYPE, "text/javascript")
    renderText(js)
  }

  def renderUpdate(id: String, value: Any) {
    val js = jsUpdate(id, value)
    renderJS(js)
  }
}
