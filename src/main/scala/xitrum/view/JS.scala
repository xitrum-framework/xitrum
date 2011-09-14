package xitrum.view

import scala.xml.Unparsed

import xitrum.{Action, Config}
import xitrum.comet.CometGetAction
import xitrum.routing.Routes

trait JS {
  this: Action =>

  private val buffer = new StringBuilder

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

  def jsCometGet(channel: String, callback: String) {
    val encryptedChannel       = validate("channel")
    val encryptedLastTimestamp = validate("lastTimestamp")

    // http://stackoverflow.com/questions/2703861/chromes-loading-indicator-keeps-spinning-during-xmlhttprequest
    // http://stackoverflow.com/questions/1735560/stop-the-browser-throbber-of-doom-while-loading-comet-server-push-xmlhttpreques
    jsAddToView("setTimeout(function () { xitrum.cometGet('" + encryptedChannel + "', '" + channel + "', '" + encryptedLastTimestamp + "', 0, " + callback + ") }, 1000)")
  }

  //----------------------------------------------------------------------------

  def jsAtBottom = {
    val jsForView = <script>{Unparsed("$(function() {\n" + buffer.toString + "});")}</script>

    if (Config.isProductionMode)
      <xml:group>
        <script type="text/javascript" src={urlForResource("xitrum/jquery-1.6.2.min.js")}></script>
        <script type="text/javascript" src={urlForResource("xitrum/jquery.validate-1.8.1/jquery.validate.min.js")}></script>
        <script type="text/javascript" src={urlForResource("xitrum/jquery.validate-1.8.1/additional-methods.min.js")}></script>
        {if (getLanguage != "en") <script type="text/javascript" src={urlForResource("xitrum/jquery.validate-1.8.1/localization/messages_"+ getLanguage +".js")}></script>}
        <script type="text/javascript" src={urlForResource("xitrum/xitrum.js")}></script>
        <script type="text/javascript" src={urlFor[JSRoutesAction]}></script>
        {jsForView}
      </xml:group>
    else
      <xml:group>
        <script type="text/javascript" src={urlForResource("xitrum/jquery-1.6.2.js")}></script>
        <script type="text/javascript" src={urlForResource("xitrum/jquery.validate-1.8.1/jquery.validate.js")}></script>
        <script type="text/javascript" src={urlForResource("xitrum/jquery.validate-1.8.1/additional-methods.js")}></script>
        {if (getLanguage != "en") <script type="text/javascript" src={urlForResource("xitrum/jquery.validate-1.8.1/localization/messages_"+ getLanguage +".js")}></script>}
        <script type="text/javascript" src={urlForResource("xitrum/xitrum.js")}></script>
        <script type="text/javascript" src={urlFor[JSRoutesAction]}></script>
        {jsForView}
      </xml:group>
  }
}
