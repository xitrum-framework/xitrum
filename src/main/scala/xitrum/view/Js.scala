package xitrum.view

import scala.xml.Unparsed

import org.apache.commons.lang3.StringEscapeUtils
import io.netty.channel.ChannelFuture

import xitrum.Action

// http://stackoverflow.com/questions/2703861/chromes-loading-indicator-keeps-spinning-during-xmlhttprequest
// http://stackoverflow.com/questions/1735560/stop-the-browser-throbber-of-doom-while-loading-comet-server-push-xmlhttpreques
trait JsRenderer {
  this: Action =>

  private val buffer = new StringBuilder

  /**
   * You can use this method to add dynamic JS snippets to a buffer, then use
   * method jsForView to take out that buffer to embed the snippets to view
   * template.
   */
  def jsAddToView(js: Any) {
    buffer.append(js.toString)
    buffer.append(";\n")
  }

  /** See jsAddToView. */
  lazy val jsForView = if (buffer.isEmpty) "" else <script type="text/javascript">{Unparsed("\n//<![CDATA[\n$(function() {\n" + buffer.toString + "});\n//]]>\n")}</script>

  lazy val jsDefaults = {
    // See directory src/main/resources/META-INF/resources/webjars/jquery-validation
    val validateI18n = if (language.contains("en")) "" else (<script type="text/javascript" src={webJarsUrl("jquery-validation/1.15.0/localization", s"messages_$language.js", s"messages_$language.min.js")}></script>)

    <xml:group>
      <script type="text/javascript" src={webJarsUrl("jquery/3.1.0/dist",         "jquery.js",             "jquery.min.js")}></script>
      <script type="text/javascript" src={webJarsUrl("jquery-validation/1.15.0",  "jquery.validate.js",    "jquery.validate.min.js")}></script>
      <script type="text/javascript" src={webJarsUrl("jquery-validation/1.15.0",  "additional-methods.js", "additional-methods.min.js")}></script>
      {validateI18n}
      <script type="text/javascript" src={webJarsUrl("sockjs-client/1.1.1/dist",  "sockjs.js",             "sockjs.min.js")}></script>
      <script type="text/javascript" src={url[xitrum.js]}></script>
    </xml:group>
  }

  //----------------------------------------------------------------------------

  /**
   * Do not use this to escape JSON, because they are different! For example
   * JSON does not escape ' character, while JavaScript does. To escape JSON,
   * use xitrum.util.SeriDeseri.toJson(Seq(string)).
   *
   * org.apache.commons.lang3.StringEscapeUtils is used internally.
   */
  def jsEscape(string: Any) = StringEscapeUtils.escapeEcmaScript(string.toString)

  def js$(selector: String) = "$(\"" + selector + "\")"

  def js$id(id: String) = js$("#" + id)

  def js$name(name: String) = js$("[name='" + name + "']")
}

trait JsResponder {
  this: Action =>

  // lazy because request is null when this instance is created
  lazy val isAjax = request.headers.contains("X-Requested-With")

  def jsRespond(fragments: Any*): ChannelFuture = {
    val js = fragments.mkString(";\n") + ";\n"
    respondText(js, "text/javascript")
  }

  /** See http://stackoverflow.com/questions/503093/how-can-i-make-a-redirect-page-in-jquery */
  def jsRedirectTo(location: Any): ChannelFuture =
  jsRespond("window.location.href = \"" + jsEscape(location) + "\"")

  def jsRedirectTo[T <: Action : Manifest](params: (String, Any)*): ChannelFuture =
    jsRedirectTo(url[T](params:_*))
}
