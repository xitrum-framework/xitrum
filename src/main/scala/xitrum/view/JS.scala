package xitrum.view

import scala.xml.Unparsed

import xitrum.{Config, Controller}
import xitrum.comet.CometGetController
import xitrum.etag.{Etag, NotModified}
import xitrum.routing.{Route, Routes, JSRoutesController}
import xitrum.validator.Validated

trait JS {
  this: Controller =>

  private val buffer = new StringBuilder

  // lazy because request is null when this instance is created
  lazy val isAjax = request.containsHeader("X-Requested-With")

  def jsAddToView(js: Any) {
    buffer.append(js.toString)
    buffer.append(";\n")
  }

  //----------------------------------------------------------------------------

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
    renderText(js, "text/javascript; charset=" + Config.config.request.charset)
  }

  def jsRenderFormat(format: String, args: Any*) {
    val js = format.format(args:_*)
    jsRender(js)
  }

  /** See http://stackoverflow.com/questions/503093/how-can-i-make-a-redirect-page-in-jquery */
  def jsRedirectTo(location: Any) {
    jsRender("window.location.href = " + jsEscape(location))
  }

  def jsRedirectTo(route: Route) { jsRedirectTo(route.url) }

  def jsCometGet(channel: String, callback: String) {
    val encryptedChannel       = Validated.secureParamName("channel")
    val encryptedLastTimestamp = Validated.secureParamName("lastTimestamp")

    // http://stackoverflow.com/questions/2703861/chromes-loading-indicator-keeps-spinning-during-xmlhttprequest
    // http://stackoverflow.com/questions/1735560/stop-the-browser-throbber-of-doom-while-loading-comet-server-push-xmlhttpreques
    jsAddToView("setTimeout(function () { xitrum.cometGet('" + encryptedChannel + "', '" + channel + "', '" + encryptedLastTimestamp + "', 0, " + callback + ") }, 1000)")
  }

  //----------------------------------------------------------------------------

  def jsAtBottom = {
    val validatei18n = if (getLanguage == "en") "" else <script type="text/javascript" src={urlForResource("xitrum/jquery.validate-1.9.0/localization/messages_"+ getLanguage +".js")}></script>
    val jsRoutesAction = <script type="text/javascript" src={JSRoutesController.serve.url + "?" + Etag.forString(Routes.jsRoutes)}></script>
    val jsForView = if (buffer.isEmpty) "" else <script type="text/javascript">{Unparsed("\n//<![CDATA[\n$(function() {\n" + buffer.toString + "});\n//]]>\n")}</script>

    if (Config.isProductionMode)
      <xml:group>
        <script type="text/javascript" src={urlForResource("xitrum/jquery-1.7.1.min.js")}></script>
        <script type="text/javascript" src={urlForResource("xitrum/jquery.validate-1.9.0/jquery.validate.min.js")}></script>
        <script type="text/javascript" src={urlForResource("xitrum/jquery.validate-1.9.0/additional-methods.min.js")}></script>
        {validatei18n}
        <script type="text/javascript" src={urlForResource("xitrum/knockout/knockout-2.0.0.min.js")}></script>
        <script type="text/javascript" src={urlForResource("xitrum/knockout/knockout.mapping-2.0.3.min.js")}></script>
        <script type="text/javascript" src={urlForResource("xitrum/xitrum.js")}></script>
        {jsRoutesAction}
        {jsForView}
      </xml:group>
    else
      <xml:group>
        <script type="text/javascript" src={urlForResource("xitrum/jquery-1.7.1.js")}></script>
        <script type="text/javascript" src={urlForResource("xitrum/jquery.validate-1.9.0/jquery.validate.js")}></script>
        <script type="text/javascript" src={urlForResource("xitrum/jquery.validate-1.9.0/additional-methods.js")}></script>
        {validatei18n}
        <script type="text/javascript" src={urlForResource("xitrum/knockout/knockout-2.0.0.js")}></script>
        <script type="text/javascript" src={urlForResource("xitrum/knockout/knockout.mapping-2.0.3.js")}></script>
        <script type="text/javascript" src={urlForResource("xitrum/xitrum.js")}></script>
        {jsRoutesAction}
        {jsForView}
      </xml:group>
  }
}
