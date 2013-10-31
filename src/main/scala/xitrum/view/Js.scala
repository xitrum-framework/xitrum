package xitrum.view

import scala.xml.Unparsed

import org.apache.commons.lang3.StringEscapeUtils
import org.jboss.netty.channel.ChannelFuture

import xitrum.{Config, Action}
import xitrum.etag.{Etag, NotModified}

// http://stackoverflow.com/questions/2703861/chromes-loading-indicator-keeps-spinning-during-xmlhttprequest
// http://stackoverflow.com/questions/1735560/stop-the-browser-throbber-of-doom-while-loading-comet-server-push-xmlhttpreques
trait Js {
  this: Action =>

  private val buffer = new StringBuilder

  // lazy because request is null when this instance is created
  lazy val isAjax = request.containsHeader("X-Requested-With")

  def jsAddToView(js: Any) {
    buffer.append(js.toString)
    buffer.append(";\n")
  }

  //----------------------------------------------------------------------------

  /**
   * Do not use this to escape JSON, because they are different! For example
   * JSON does not escape ' character, while JavaScript does. To escape JSON,
   * use JSON4S or xitrum.util.Json, e.g xitrum.util.Json.generate(Seq(string)).
   *
   * org.apache.commons.lang3.StringEscapeUtils is used internally.
   */
  def jsEscape(string: Any) = StringEscapeUtils.escapeEcmaScript(string.toString)

  def js$(selector: String) = "$(\"" + selector + "\")"

  def js$id(id: String) = js$("#" + id)

  def js$name(name: String) = js$("[name='" + name + "']")

  //----------------------------------------------------------------------------

  def jsRespond(fragments: Any*): ChannelFuture = {
    val js = fragments.mkString(";\n") + ";\n"
    respondText(js, "text/javascript")
  }

  /** See http://stackoverflow.com/questions/503093/how-can-i-make-a-redirect-page-in-jquery */
  def jsRedirectTo(location: Any): ChannelFuture = {
    jsRespond("window.location.href = \"" + jsEscape(location) + "\"")
  }

  def jsRedirectTo[T <: Action : Manifest](params: (String, Any)*): ChannelFuture = { jsRedirectTo(url[T](params:_*)) }

  //----------------------------------------------------------------------------

  lazy val jsDefaults = {
    val validatei18n = if (getLanguage == "en") "" else <script type="text/javascript" src={resourceUrl("xitrum/jquery.validate-1.11.1/localization/messages_"+ getLanguage +".js")}></script>

    if (Config.productionMode)
      <xml:group>
        <script type="text/javascript" src={resourceUrl("xitrum/jquery-1.10.2.min.js")}></script>
        <script type="text/javascript" src={resourceUrl("xitrum/jquery.validate-1.11.1/jquery.validate.min.js")}></script>
        <script type="text/javascript" src={resourceUrl("xitrum/jquery.validate-1.11.1/additional-methods.min.js")}></script>
        {validatei18n}
        <script type="text/javascript" src={resourceUrl("xitrum/knockout/knockout-3.0.0.min.js")}></script>
        <script type="text/javascript" src={resourceUrl("xitrum/knockout/knockout.mapping-2.4.1.min.js")}></script>
        <script type="text/javascript" src={resourceUrl("xitrum/sockjs-0.3.4.min.js")}></script>
        <script type="text/javascript" src={resourceUrl("xitrum/xitrum.js")}></script>
      </xml:group>
    else
      <xml:group>
        <script type="text/javascript" src={resourceUrl("xitrum/jquery-1.10.2.js")}></script>
        <script type="text/javascript" src={resourceUrl("xitrum/jquery.validate-1.11.1/jquery.validate.js")}></script>
        <script type="text/javascript" src={resourceUrl("xitrum/jquery.validate-1.11.1/additional-methods.js")}></script>
        {validatei18n}
        <script type="text/javascript" src={resourceUrl("xitrum/knockout/knockout-3.0.0.js")}></script>
        <script type="text/javascript" src={resourceUrl("xitrum/knockout/knockout.mapping-2.4.1.js")}></script>
        <script type="text/javascript" src={resourceUrl("xitrum/sockjs-0.3.4.js")}></script>
        <script type="text/javascript" src={resourceUrl("xitrum/xitrum.js")}></script>
      </xml:group>
  }

  lazy val jsForView = if (buffer.isEmpty) "" else <script type="text/javascript">{Unparsed("\n//<![CDATA[\n$(function() {\n" + buffer.toString + "});\n//]]>\n")}</script>
}
