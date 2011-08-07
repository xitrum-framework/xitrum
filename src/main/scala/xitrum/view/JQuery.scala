package xitrum.view

import org.jboss.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE

import xitrum.{Action, Config}
import xitrum.scope.session.CSRF

trait JQuery {
  this: Action =>

  lazy val xitrumHead =
    if (Config.isProductionMode)
      <xml:group>
        <link href={urlForResource("xitrum/xitrum.css")} type="text/css" rel="stylesheet" media="all"></link>
        <script type="text/javascript" src={urlForResource("xitrum/jquery-1.6.2.min.js")}></script>
        <script type="text/javascript" src={urlForResource("xitrum/jquery.validate-1.8.1/jquery.validate.min.js")}></script>
        <script type="text/javascript" src={urlForResource("xitrum/jquery.validate-1.8.1/additional-methods.min.js")}></script>
        <script type="text/javascript" src={urlForResource("xitrum/xitrum.js")}></script>
        <meta name={CSRF.TOKEN} content={antiCSRFToken} />
      </xml:group>
    else
      <xml:group>
        <link href={urlForResource("xitrum/xitrum.css")} type="text/css" rel="stylesheet" media="all"></link>
        <script type="text/javascript" src={urlForResource("xitrum/jquery-1.6.2.js")}></script>
        <script type="text/javascript" src={urlForResource("xitrum/jquery.validate-1.8.1/jquery.validate.js")}></script>
        <script type="text/javascript" src={urlForResource("xitrum/jquery.validate-1.8.1/additional-methods.js")}></script>
        <script type="text/javascript" src={urlForResource("xitrum/xitrum.js")}></script>
        <meta name={CSRF.TOKEN} content={antiCSRFToken} />
      </xml:group>

  /** See escape_javascript of Rails */
  def jsEscape(string: Any) = {
    val escaped = string.toString
      .replace("\\\\", "\\0\\0")
      .replace("</",   "<\\/")
      .replace("\r\n", "\\n")
      .replace("\n",   "\\n")
      .replace("\r",   "\\n")
      .replace("\"",   "\\\"")
      .replace("'",    "\\'")

    // The result needs to be wrapped with double quote, not single quote
    "\"" + escaped + "\""
  }

  def js$(selector: String) = "$(\"" + selector + "\")"

  def js$id(id: String) = js$("#" + id)

  def js$name(name: String) = js$("[name='" + name + "']")

  //----------------------------------------------------------------------------

  def jsRender(fragments: Any*) {
    val js = fragments.mkString(";\n") + ";\n"
    renderText(js, "text/javascript")
  }

  def jsRenderFormat(format: String, args: Any*) {
    val js = format.format(args:_*)
    jsRender(js)
  }

  /** See http://stackoverflow.com/questions/503093/how-can-i-make-a-redirect-page-in-jquery */
  def jsRedirectTo(location: Any) {
    jsRender("window.location.href = " + jsEscape(location))
  }

  def jsRedirectTo[T: Manifest] { jsRedirectTo(urlFor[T]) }
}
