package xt.vc.view

import org.jboss.netty.handler.codec.http.HttpHeaders.Names._
import xt.Action

trait JQuery {
  this: Action =>

  lazy val jsHead = {
    <script type="text/javascript" src="/resources/public/xt/jquery-1.5.min.js"></script>
    <script type="text/javascript" src="/resources/public/xt/xitrum.js"></script>
    <script>{"var xt_csrf_token = '" + csrfToken + "';"}</script>
  }

  /** See escape_javascript of Rails */
  def jsEscape(value: Any) = value.toString
    .replace("\\\\", "\\0\\0")
    .replace("</",   "<\\/")
    .replace("\r\n", "\\n")
    .replace("\n",   "\\n")
    .replace("\r",   "\\n")
    .replace("\"",   "\\\"")
    .replace("'",    "\\'")

  def jsCall(function: String, args: String*) =
    function + "(\"" + args.map(jsEscape _).mkString(", ") + "\")"

  def jsChain(jsCalls: String*) = jsCalls.mkString(".")

  def jsUpdate(id: String, value: Any) =
    jsChain(
      jsCall("$", "#" + id),
      jsCall("html", value.toString)
    )

  def jsRender(values: String*) {
    val js = values.mkString(";\n")
    response.setHeader(CONTENT_TYPE, "text/javascript")
    renderText(js)
  }

  def jsRenderUpdate(id: String, value: Any) {
    jsRender(jsUpdate(id, value))
  }

  /** See http://stackoverflow.com/questions/503093/how-can-i-make-a-redirect-page-in-jquery */
  def jsRedirectTo(location: String) {
    jsRender("window.location.href = \"" + jsEscape(location) + "\"")
  }
}
