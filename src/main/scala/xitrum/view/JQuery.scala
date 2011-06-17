package xitrum.view

import org.jboss.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE

import xitrum.{Action, Config}

trait JQuery {
  this: Action =>

  lazy val xitrumHead =
    if (Config.isProductionMode) {
      <link href="/resources/public/xitrum/xitrum.css" type="text/css" rel="stylesheet" media="all"></link>
      <script type="text/javascript" src="/resources/public/xitrum/jquery-1.6.min.js"></script>
      <script type="text/javascript" src="/resources/public/xitrum/jquery.validate.pack-SNAPSHOT.js"></script>
      <script type="text/javascript" src="/resources/public/xitrum/xitrum.js"></script>
    } else {
      <link href="/resources/public/xitrum/xitrum.css" type="text/css" rel="stylesheet" media="all"></link>
      <script type="text/javascript" src="/resources/public/xitrum/jquery-1.6.js"></script>
      <script type="text/javascript" src="/resources/public/xitrum/jquery.validate-SNAPSHOT.js"></script>
      <script type="text/javascript" src="/resources/public/xitrum/xitrum.js"></script>
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
    function + "(" + args.mkString(", ") + ")"

  def js$(s: String) = jsCall("$", s)

  def jsById(id: String) = js$("\"#" + id + "\"")

  def jsByName(name: String) = js$("\"[name='" + name + "']\"")

  def jsChain(jsCalls: String*) = jsCalls.mkString(".")

  def jsHtml(selector: String, value: Any) =
    jsChain(
      selector,
      jsCall("html", "\"" + jsEscape(value.toString) + "\"")
    )

  def jsVal(selector: String, value: Any) =
    jsChain(
      selector,
      jsCall("val", "\"" + jsEscape(value.toString) + "\"")
    )

  def jsBefore(selector: String, value: Any) = {
    jsChain(
      selector,
      jsCall("before", "\"" + jsEscape(value.toString) + "\"")
    )
  }

  //----------------------------------------------------------------------------

  def jsRender(values: String*) {
    val js = values.mkString(";\n") + ";\n"
    renderText(js, "text/javascript")
  }

  def jsRenderCall(function: String, args: String*) {
    val js = jsCall(function, args:_*)
    jsRender(js)
  }

  def jsRenderHtml(selector: String, value: Any) {
    jsRender(jsHtml(selector, value))
  }

  //----------------------------------------------------------------------------

  /** See http://stackoverflow.com/questions/503093/how-can-i-make-a-redirect-page-in-jquery */
  def jsRedirectTo(location: String) {
    jsRender("window.location.href = \"" + jsEscape(location) + "\"")
  }

  def jsRedirectTo[T: Manifest] { jsRedirectTo(urlFor[T]) }
}
