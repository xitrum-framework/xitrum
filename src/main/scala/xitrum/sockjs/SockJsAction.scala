package xitrum.sockjs

import java.util.{Arrays, Random}
import scala.util.control.NonFatal

import org.jboss.netty.channel.ChannelFutureListener
import org.jboss.netty.buffer.ChannelBuffers
import org.jboss.netty.handler.codec.http.{DefaultCookie, HttpHeaders, HttpResponseStatus}

import akka.actor.{Actor, ActorRef, PoisonPill, Props, Terminated}

import xitrum.{Action, ActionActor, Config, SkipCSRFCheck}
import xitrum.annotation._
import xitrum.etag.NotModified
import xitrum.routing.Routes
import xitrum.util.{Json, Lookup, LookupOrCreate, ClusterSingletonActor}
import xitrum.view.DocType

// General info:
// http://sockjs.github.com/sockjs-protocol/sockjs-protocol-0.3.html
// http://en.wikipedia.org/wiki/Cross-origin_resource_sharing
// https://developer.mozilla.org/en-US/docs/HTTP_access_control
//
// Reference implementation (need to read when in doubt):
// https://github.com/sockjs/sockjs-node/tree/master/src
object SockJsAction {
  val LIMIT = if (Config.productionMode) 128 * 1024 else 4 * 1024

  //----------------------------------------------------------------------------

  private val random = new Random(System.currentTimeMillis())

  /** 0 to 2^32 - 1 */
  def entropy() = random.nextInt().abs

  //----------------------------------------------------------------------------

  /** 2KB of 'h' characters */
  val h2KB = {
    val ret = ChannelBuffers.buffer(2048 + 1)
    for (i <- 1 to 2048) ret.writeByte('h')
    ret.writeByte('\n')
    ret
  }

  /** Template for htmlfile transport */
  def htmlfile(callback: String, with1024: Boolean): String = {
    val template = """<!doctype html>
<html><head>
  <meta http-equiv="X-UA-Compatible" content="IE=edge" />
  <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
</head><body><h2>Don't panic!</h2>
  <script>
    document.domain = document.domain;
    var c = parent.""" + callback + """;
    c.start();
    function p(d) {c.message(d);};
    window.onload = function() {c.stop();};
  </script>"""

    // Safari needs at least 1024 bytes to parse the website:
    // http://code.google.com/p/browsersec/wiki/Part2#Survey_of_content_sniffing_behaviors

    // https://github.com/sockjs/sockjs-node/blob/master/src/trans-htmlfile.coffee#L29
    // http://stackoverflow.com/questions/2804827/create-a-string-with-n-characters
    if (with1024) {
      val spaces = new Array[Char](1024 - template.length + 14)
      Arrays.fill(spaces, ' ')
      template + new String(spaces) + "\r\n\r\n"
    } else {
      template
    }
  }

  //----------------------------------------------------------------------------

  // https://groups.google.com/group/sockjs/msg/9da24b0dde8916e4
  // https://groups.google.com/group/sockjs/msg/b63cd4555bd69ae4
  // https://github.com/sockjs/sockjs-node/blob/master/src/utils.coffee#L87-L109
  def quoteUnicode(string: String): String = {
    val b = new StringBuilder
    string.foreach { c =>
      if (('\u0000' <= c && c <= '\u001f') ||
          ('\ud800' <= c && c <= '\udfff') ||
          ('\u200c' <= c && c <= '\u200f') ||
          ('\u2028' <= c && c <= '\u202f') ||
          ('\u2060' <= c && c <= '\u206f') ||
          ('\ufff0' <= c && c <= '\uffff')) {
        val hex = Integer.toHexString(c)
        val len = hex.length

        b.append("\\u")
        if (len == 1)
          b.append("000")
        else if (len == 2)
          b.append("00")
        else if (len == 3)
          b.append("0")

        b.append(hex)
      } else {
        b.append(c)
      }
    }
    b.toString
  }
}

trait SockJsAction extends Action with SkipCSRFCheck {
  // Set by Dispatcher
  var pathPrefix = ""

  // JSESSIONID cookie must be echoed back if sent by the client, or created
  // http://groups.google.com/group/sockjs/browse_thread/thread/71dfdff6e8f1e5f7
  // Can't use beforeFilter, see comment of pathPrefix at the top of this controller.
  protected def handleCookie() {
    val sockJsClassAndOptions = Routes.sockJsClassAndOptions(pathPrefix)
    if (sockJsClassAndOptions.cookieNeeded) {
      val value  = requestCookies.get("JSESSIONID").getOrElse("dummy")
      val cookie = new DefaultCookie("JSESSIONID", value)
      responseCookies.append(cookie)
    }
  }

  protected def setCORS() {
    val requestOrigin  = request.getHeader(HttpHeaders.Names.ORIGIN)
    val responseOrigin = if (requestOrigin == null || requestOrigin == "null") "*" else requestOrigin
    response.setHeader(HttpHeaders.Names.ACCESS_CONTROL_ALLOW_ORIGIN,      responseOrigin)
    response.setHeader(HttpHeaders.Names.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true")

    val accessControlRequestHeaders = request.getHeader(HttpHeaders.Names.ACCESS_CONTROL_REQUEST_HEADERS)
    if (accessControlRequestHeaders != null)
      response.setHeader(HttpHeaders.Names.ACCESS_CONTROL_ALLOW_HEADERS, accessControlRequestHeaders)
  }

  protected def xhrOPTIONS() {
    response.setStatus(HttpResponseStatus.NO_CONTENT)
    response.setHeader(HttpHeaders.Names.ACCESS_CONTROL_ALLOW_METHODS, "OPTIONS, POST")
    setCORS()
    setClientCacheAggressively()
    respond()
  }

  protected def callbackParam(): Option[String] = {
    val paramName = if (handlerEnv.uriParams.isDefinedAt("c")) "c" else "callback"
    val ret = paramo(paramName)
    if (ret == None) {
      response.setStatus(HttpResponseStatus.INTERNAL_SERVER_ERROR)
      respondText("\"callback\" parameter required")
    }
    ret
  }

  //----------------------------------------------------------------------------

  /**
   * We should close a streaming request every 128KB messages was send.
   * The test server should have this limit decreased to 4KB.
   */
  private[this] var streamingBytesSent = 0

  /**
   * All the chunking transports are closed by the server after 128K was
   * send, in order to force client to GC and reconnect. The server doesn't have
   * to send "c" frame.
   *
   * @return false if the channel will be closed when the channel write completes
   */
  protected def respondStreamingWithLimit(text: String, isEventSource: Boolean = false): Boolean = {
    // This is length in characters, not bytes,
    // but in this case the result doesn't have to be precise
    val size = text.length
    streamingBytesSent += size
    if (streamingBytesSent < SockJsAction.LIMIT) {
      if (isEventSource) respondEventSource(text) else respondText(text)
      true
    } else {
      if (isEventSource) respondEventSource(text) else respondText(text)
      respondLastChunk().addListener(ChannelFutureListener.CLOSE)
      false
    }
  }
}

trait SockJsActionActor extends ActionActor with SockJsAction {
  protected def lookupOrCreateNonWebSocketSessionActor(sessionId: String) {
    val propsMaker = () => Props(new NonWebSocketSession(self, pathPrefix, this))
    ClusterSingletonActor.actor() ! LookupOrCreate(sessionId, propsMaker)
  }

  protected def lookupNonWebSocketSessionActor(sessionId: String) {
    ClusterSingletonActor.actor() ! Lookup(sessionId)
  }
}

@GET("")
class SockJsGreeting extends SockJsAction {
  def execute() {
    respondText("Welcome to SockJS!\n")
  }
}

@Last
@GET(":iframe")
class SockJsIframe extends SockJsAction {
  def execute() {
    val iframe = param("iframe")
    if (iframe.startsWith("iframe") && iframe.endsWith(".html")) {
      val src =
        if (Config.productionMode)
          "xitrum/sockjs-0.3.4.min.js"
        else
          "xitrum/sockjs-0.3.4.js"

      setClientCacheAggressively()
      respondHtml(DocType.html5(
<html>
<head>
  <meta http-equiv="X-UA-Compatible" content="IE=edge" />
  <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
  <script><xml:unparsed>
    document.domain = document.domain;
    _sockjs_onload = function(){SockJS.bootstrap_iframe();};
  </xml:unparsed></script>
  <script src={urlForResource(src)}></script>
</head>
<body>
  <h2>Don't panic!</h2>
  <p>This is a SockJS hidden iframe. It's used for cross domain magic.</p>
</body>
</html>
      ))
    } else {
      respondDefault404Page()
    }
  }
}

@GET("info")
class SockJsInfoGET extends SockJsAction {
  def execute() {
    setCORS()
    setNoClientCache()
    // FIXME: Retest if IE works when cookie_needed is set to true
    val sockJsClassAndOptions = Routes.sockJsClassAndOptions(pathPrefix)
    respondJsonText(
      """{"websocket": """      + sockJsClassAndOptions.websocket +
      """, "cookie_needed": """ + sockJsClassAndOptions.cookieNeeded +
      """, "origins": ["*:*"], "entropy": """ + SockJsAction.entropy() + "}"
    )
  }
}

@OPTIONS("info")
class SockJsInfoOPTIONS extends SockJsAction {
  def execute() {
    response.setStatus(HttpResponseStatus.NO_CONTENT)
    response.setHeader(HttpHeaders.Names.ACCESS_CONTROL_ALLOW_METHODS, "OPTIONS, GET")
    setCORS()
    setClientCacheAggressively()
    respond()
  }
}

@OPTIONS(":serverId<[^\\.]+>/:sessionId<[^\\.]+>/xhr")
class SockJsXhrPollingOPTIONSReceive extends SockJsAction {
  def execute() {
    xhrOPTIONS()
  }
}

@OPTIONS(":serverId<[^\\.]+>/:sessionId<[^\\.]+>/xhr_send")
class SockJsXhrPollingOPTIONSSend extends SockJsAction {
  def execute() {
    xhrOPTIONS()
  }
}

@POST(":serverId<[^\\.]+>/:sessionId<[^\\.]+>/xhr")
class SockJsXhrPollingReceive extends SockJsActionActor {
  def execute() {
    val sessionId = param("sessionId")

    handleCookie()
    setCORS()
    setNoClientCache()

    lookupOrCreateNonWebSocketSessionActor(sessionId)
    context.become {
      case (newlyCreated: Boolean, nonWebSocketSession: ActorRef) =>
        if (newlyCreated) {
          respondJs("o\n")
        } else {
          nonWebSocketSession ! SubscribeByClient
          context.watch(nonWebSocketSession)
          context.become(receiveSubscribeResult(nonWebSocketSession))
        }
    }
  }

  private def receiveSubscribeResult(nonWebSocketSession: ActorRef): Receive = {
    case SubscribeResultToClientAnotherConnectionStillOpen =>
      respondJs("c[2010,\"Another connection still open\"]\n")
      .addListener(ChannelFutureListener.CLOSE)

    case SubscribeResultToClientClosed =>
      respondJs("c[3000,\"Go away!\"]\n")
      .addListener(ChannelFutureListener.CLOSE)

    case SubscribeResultToClientMessages(messages) =>
      val json   = Json.generate(messages)
      val quoted = SockJsAction.quoteUnicode(json)
      respondJs("a" + quoted + "\n")

    case SubscribeResultToClientWaitForMessage =>
      context.become(receiveNotification(nonWebSocketSession))

    case Terminated(`nonWebSocketSession`) =>
      respondJs("c[2011,\"Server error\"]\n")
      .addListener(ChannelFutureListener.CLOSE)
  }

  private def receiveNotification(nonWebSocketSession: ActorRef): Receive = {
    case NotificationToClientMessage(message) =>
      val json   = Json.generate(Seq(message))
      val quoted = SockJsAction.quoteUnicode(json)
      respondJs("a" + quoted + "\n")

    case NotificationToClientHeartbeat =>
      respondJs("h\n")

    case NotificationToClientClosed =>
      respondJs("c[3000,\"Go away!\"]\n")
      .addListener(ChannelFutureListener.CLOSE)

    case Terminated(`nonWebSocketSession`) =>
      respondJs("c[2011,\"Server error\"]\n")
      .addListener(ChannelFutureListener.CLOSE)
  }
}

@POST(":serverId<[^\\.]+>/:sessionId<[^\\.]+>/xhr_send")
class SockJsXhrSend extends SockJsActionActor {
  def execute() {
    val body = request.getContent.toString(Config.requestCharset)
    if (body.isEmpty) {
      response.setStatus(HttpResponseStatus.INTERNAL_SERVER_ERROR)
      respondText("Payload expected.")
      return
    }

    val messages: Seq[String] = try {
      // body: ["m1", "m2"]
      Json.parse[Seq[String]](body)
    } catch {
      case NonFatal(e) =>
        response.setStatus(HttpResponseStatus.INTERNAL_SERVER_ERROR)
        respondText("Broken JSON encoding.")
        return
    }

    val sessionId = param("sessionId")
    lookupNonWebSocketSessionActor(sessionId)
    context.become {
      case None =>
        respondDefault404Page()

      case Some(nonWebSocketSession: ActorRef) =>
        nonWebSocketSession ! SendMessagesByClient(messages)
        response.setStatus(HttpResponseStatus.NO_CONTENT)
        response.setHeader(HttpHeaders.Names.CONTENT_TYPE, "text/plain; charset=UTF-8")
        setCORS()
        respond()
    }
  }
}

@OPTIONS(":serverId<[^\\.]+>/:sessionId<[^\\.]+>/xhr_streaming")
class SockJsXhrStreamingOPTIONSReceive extends SockJsAction {
  def execute() {
    xhrOPTIONS()
  }
}

@POST(":serverId<[^\\.]+>/:sessionId<[^\\.]+>/xhr_streaming")
class SockJsXhrStreamingReceive extends SockJsActionActor {
  def execute() {
    val sessionId = param("sessionId")

    handleCookie()
    setCORS()
    setNoClientCache()

    // There's always 2KB prelude, even for immediate close frame
    response.setChunked(true)
    response.setHeader(HttpHeaders.Names.CONTENT_TYPE, "application/javascript; charset=" + Config.xitrum.request.charset)
    respondBinary(SockJsAction.h2KB)

    lookupOrCreateNonWebSocketSessionActor(sessionId)
    context.become {
      case (newlyCreated: Boolean, nonWebSocketSession: ActorRef) =>
        context.watch(nonWebSocketSession)
        if (newlyCreated) {
          respondStreamingWithLimit("o\n")
          context.become(receiveNotification(nonWebSocketSession))
        } else {
          nonWebSocketSession ! SubscribeByClient
          context.become(receiveSubscribeResult(nonWebSocketSession))
        }
    }
  }

  private def receiveSubscribeResult(nonWebSocketSession: ActorRef): Receive = {
    case SubscribeResultToClientAnotherConnectionStillOpen =>
      respondJs("c[2010,\"Another connection still open\"]\n")
      respondLastChunk()
      .addListener(ChannelFutureListener.CLOSE)

    case SubscribeResultToClientClosed =>
      respondJs("c[3000,\"Go away!\"]\n")
      respondLastChunk()
      .addListener(ChannelFutureListener.CLOSE)

    case SubscribeResultToClientMessages(messages) =>
      val json   = Json.generate(messages)
      val quoted = SockJsAction.quoteUnicode(json)
      if (respondStreamingWithLimit("a" + quoted + "\n"))
        context.become(receiveNotification(nonWebSocketSession))

    case SubscribeResultToClientWaitForMessage =>
      context.become(receiveNotification(nonWebSocketSession))

    case Terminated(`nonWebSocketSession`) =>
      respondJs("c[2011,\"Server error\"]\n")
      respondLastChunk()
      .addListener(ChannelFutureListener.CLOSE)
  }

  private def receiveNotification(nonWebSocketSession: ActorRef): Receive = {
    case NotificationToClientMessage(message) =>
      val json   = Json.generate(Seq(message))
      val quoted = SockJsAction.quoteUnicode(json)
      respondStreamingWithLimit("a" + quoted + "\n")

    case NotificationToClientHeartbeat =>
      respondStreamingWithLimit("h\n")

    case NotificationToClientClosed =>
      respondJs("c[3000,\"Go away!\"]\n")
      respondLastChunk()
      .addListener(ChannelFutureListener.CLOSE)

    case Terminated(`nonWebSocketSession`) =>
      respondJs("c[2011,\"Server error\"]\n")
      respondLastChunk()
      .addListener(ChannelFutureListener.CLOSE)
  }
}

@GET(":serverId<[^\\.]+>/:sessionId<[^\\.]+>/htmlfile")
class SockJshtmlfileReceive extends SockJsActionActor {
  var callback: String = null

  def execute() {
    val callbacko = callbackParam()
    if (callbacko.isEmpty) return

    callback = callbacko.get
    val sessionId = param("sessionId")

    handleCookie()
    setCORS()
    setNoClientCache()

    lookupOrCreateNonWebSocketSessionActor(sessionId)
    context.become {
      case (newlyCreated: Boolean, nonWebSocketSession: ActorRef) =>
        context.watch(nonWebSocketSession)
        if (newlyCreated) {
          response.setChunked(true)
          respondHtml(SockJsAction.htmlfile(callback, true))
          respondText("<script>\np(\"o\");\n</script>\r\n")
          context.become(receiveNotification(nonWebSocketSession))
        } else {
          nonWebSocketSession ! SubscribeByClient
          context.become(receiveSubscribeResult(nonWebSocketSession))
        }
    }
  }

  private def receiveSubscribeResult(nonWebSocketSession: ActorRef): Receive = {
    case SubscribeResultToClientAnotherConnectionStillOpen =>
      respondHtml(
        SockJsAction.htmlfile(callback, false) +
        "<script>\np(\"c[2010,\\\"Another connection still open\\\"]\");\n</script>\r\n"
      )
      .addListener(ChannelFutureListener.CLOSE)

    case SubscribeResultToClientClosed =>
      respondHtml(
        SockJsAction.htmlfile(callback, false) +
        "<script>\np(\"c[3000,\\\"Go away!\\\"]\");\n</script>\r\n"
      )
      .addListener(ChannelFutureListener.CLOSE)

    case SubscribeResultToClientMessages(messages) =>
      val buffer = new StringBuilder
      val json   = Json.generate(messages)
      val quoted = SockJsAction.quoteUnicode(json)
      buffer.append("<script>\np(\"a")
      buffer.append(jsEscape(quoted))
      buffer.append("\");\n</script>\r\n")
      response.setChunked(true)
      respondHtml(SockJsAction.htmlfile(callback, true))
      if (respondStreamingWithLimit(buffer.toString))
        context.become(receiveSubscribeResult(nonWebSocketSession))

    case SubscribeResultToClientWaitForMessage =>
      response.setChunked(true)
      respondHtml(SockJsAction.htmlfile(callback, true))
      context.become(receiveSubscribeResult(nonWebSocketSession))

    case Terminated(`nonWebSocketSession`) =>
      respondHtml(
        SockJsAction.htmlfile(callback, false) +
        "<script>\np(\"c[2011,\\\"Server error\\\"]\");\n</script>\r\n"
      )
      .addListener(ChannelFutureListener.CLOSE)
  }

  private def receiveNotification(nonWebSocketSession: ActorRef): Receive = {
    case NotificationToClientMessage(message) =>
      val buffer = new StringBuilder
      val json   = Json.generate(Seq(message))
      val quoted = SockJsAction.quoteUnicode(json)
      buffer.append("<script>\np(\"a")
      buffer.append(jsEscape(quoted))
      buffer.append("\");\n</script>\r\n")
      respondStreamingWithLimit(buffer.toString)

    case NotificationToClientHeartbeat =>
      respondStreamingWithLimit("<script>\np(\"h\");\n</script>\r\n")

    case NotificationToClientClosed =>
      respondHtml(
        SockJsAction.htmlfile(callback, false) +
        "<script>\np(\"c[3000,\\\"Go away!\\\"]\");\n</script>\r\n"
      )
      respondLastChunk()
      .addListener(ChannelFutureListener.CLOSE)

    case Terminated(`nonWebSocketSession`) =>
      respondHtml(
        SockJsAction.htmlfile(callback, false) +
        "<script>\np(\"c[2011,\\\"Server error\\\"]\");\n</script>\r\n"
      )
      respondLastChunk()
      .addListener(ChannelFutureListener.CLOSE)
  }
}

@GET(":serverId<[^\\.]+>/:sessionId<[^\\.]+>/jsonp")
class SockJsJsonPPollingReceive extends SockJsActionActor {
  var callback: String = null

  def execute() {
    val callbacko = callbackParam()
    if (callbacko.isEmpty) return

    callback  = callbacko.get
    val sessionId = param("sessionId")

    handleCookie()
    setCORS()
    setNoClientCache()

    lookupOrCreateNonWebSocketSessionActor(sessionId)
    context.become {
      case (newlyCreated: Boolean, nonWebSocketSession: ActorRef) =>
        if (newlyCreated) {
          respondJs(callback + "(\"o\");\r\n")
        } else {
          nonWebSocketSession ! SubscribeByClient
          context.watch(nonWebSocketSession)
          context.become(receiveSubscribeResult(nonWebSocketSession))
        }
    }
  }

  private def receiveSubscribeResult(nonWebSocketSession: ActorRef): Receive = {
    case SubscribeResultToClientAnotherConnectionStillOpen =>
      respondJs(callback + "(\"c[2010,\\\"Another connection still open\\\"]\");\r\n")
      .addListener(ChannelFutureListener.CLOSE)

    case SubscribeResultToClientClosed =>
      respondJs(callback + "(\"c[3000,\\\"Go away!\\\"]\");\r\n")
      .addListener(ChannelFutureListener.CLOSE)

    case SubscribeResultToClientMessages(messages) =>
      val buffer = new StringBuilder
      val json   = Json.generate(messages)
      val quoted = SockJsAction.quoteUnicode(json)
      buffer.append(callback + "(\"a")
      buffer.append(jsEscape(quoted))
      buffer.append("\");\r\n")
      respondJs(buffer.toString)

    case SubscribeResultToClientWaitForMessage =>
      context.become(receiveSubscribeResult(nonWebSocketSession))

    case Terminated(`nonWebSocketSession`) =>
      respondJs(callback + "(\"c[2011,\\\"Server error\\\"]\");\r\n")
      .addListener(ChannelFutureListener.CLOSE)
  }

  private def receiveNotification(nonWebSocketSession: ActorRef): Receive = {
    case NotificationToClientMessage(message) =>
      val buffer = new StringBuilder
      val json   = Json.generate(Seq(message))
      val quoted = SockJsAction.quoteUnicode(json)
      buffer.append(callback + "(\"a")
      buffer.append(jsEscape(quoted))
      buffer.append("\");\r\n")
      respondJs(buffer.toString)

    case NotificationToClientHeartbeat =>
      respondJs(callback + "(\"h\");\r\n")

    case NotificationToClientClosed =>
      respondJs(callback + "(\"c[3000,\\\"Go away!\\\"]\");\r\n")
      .addListener(ChannelFutureListener.CLOSE)

    case Terminated(`nonWebSocketSession`) =>
      respondJs(callback + "(\"c[2011,\\\"Server error\\\"]\");\r\n")
      .addListener(ChannelFutureListener.CLOSE)
  }
}

@POST(":serverId<[^\\.]+>/:sessionId<[^\\.]+>/jsonp_send")
class SockJsJsonPPollingSend extends SockJsActionActor {
  def execute() {
    val body: String = try {
      val contentType = request.getHeader(HttpHeaders.Names.CONTENT_TYPE)
      if (contentType != null && contentType.toLowerCase.startsWith(HttpHeaders.Values.APPLICATION_X_WWW_FORM_URLENCODED)) {
        param("d")
      } else {
        request.getContent.toString(Config.requestCharset)
      }
    } catch {
      case NonFatal(e) =>
        ""
    }

    handleCookie()
    if (body.isEmpty) {
      response.setStatus(HttpResponseStatus.INTERNAL_SERVER_ERROR)
      respondText("Payload expected.")
      return
    }

    val sessionId = param("sessionId")

    val messages: Seq[String] = try {
      // body: ["m1", "m2"]
      Json.parse[Seq[String]](body)
    } catch {
      case NonFatal(e) =>
        response.setStatus(HttpResponseStatus.INTERNAL_SERVER_ERROR)
        respondText("Broken JSON encoding.")
        return
    }

    lookupNonWebSocketSessionActor(sessionId)
    context.become {
      case None =>
        respondDefault404Page()

      case Some(nonWebSocketSession: ActorRef) =>
        nonWebSocketSession ! SendMessagesByClient(messages)
        // Konqueror does weird things on 204.
        // As a workaround we need to respond with something - let it be the string "ok".
        setCORS()
        setNoClientCache()
        respondText("ok")
    }
  }
}

@GET(":serverId<[^\\.]+>/:sessionId<[^\\.]+>/eventsource")
class SockJEventSourceReceive extends SockJsActionActor {
  def execute() {
    val sessionId = param("sessionId")

    handleCookie()
    setCORS()
    setNoClientCache()

    lookupOrCreateNonWebSocketSessionActor(sessionId)
    context.become {
      case (newlyCreated: Boolean, nonWebSocketSession: ActorRef) =>
        context.watch(nonWebSocketSession)
        if (newlyCreated) {
          respondEventSource("o")
          context.become(receiveNotification(nonWebSocketSession))
        } else {
          nonWebSocketSession ! SubscribeByClient
          context.become(receiveSubscribeResult(nonWebSocketSession))
        }
    }
  }

  private def receiveSubscribeResult(nonWebSocketSession: ActorRef): Receive = {
    case SubscribeResultToClientAnotherConnectionStillOpen =>
      respondJs("c[2010,\"Another connection still open\"]\n")
      .addListener(ChannelFutureListener.CLOSE)

    case SubscribeResultToClientClosed =>
      respondJs("c[3000,\"Go away!\"]\n")
      .addListener(ChannelFutureListener.CLOSE)

    case SubscribeResultToClientMessages(messages) =>
      val json   = "a" + Json.generate(messages)
      val quoted = SockJsAction.quoteUnicode(json)
      if (respondStreamingWithLimit(quoted, true))
        context.become(receiveNotification(nonWebSocketSession))

    case SubscribeResultToClientWaitForMessage =>
      context.become(receiveNotification(nonWebSocketSession))

    case Terminated(`nonWebSocketSession`) =>
      respondJs("c[2011,\"Server error\"]\n")
      .addListener(ChannelFutureListener.CLOSE)
  }

  private def receiveNotification(nonWebSocketSession: ActorRef): Receive = {
    case NotificationToClientMessage(message) =>
      val json   = "a" + Json.generate(Seq(message))
      val quoted = SockJsAction.quoteUnicode(json)
      respondStreamingWithLimit(quoted, true)

    case NotificationToClientHeartbeat =>
      respondStreamingWithLimit("h", true)

    case NotificationToClientClosed =>
      respondJs("c[3000,\"Go away!\"]\n")
      respondLastChunk()
      .addListener(ChannelFutureListener.CLOSE)

    case Terminated(`nonWebSocketSession`) =>
      respondEventSource("c[2011,\"Server error\"]")
      respondLastChunk()
      .addListener(ChannelFutureListener.CLOSE)
  }
}

// http://sockjs.github.com/sockjs-protocol/sockjs-protocol-0.3.3.html#section-52
@Last
@GET(":serverId<[^\\.]+>/:sessionId<[^\\.]+>/websocket")
class SockJSWebsocketGET extends SockJsAction {
  def execute() {
    response.setStatus(HttpResponseStatus.BAD_REQUEST)
    respondText("""'Can "Upgrade" only to "WebSocket".'""")
  }
}

// http://sockjs.github.com/sockjs-protocol/sockjs-protocol-0.3.3.html#section-54
// http://sockjs.github.com/sockjs-protocol/sockjs-protocol-0.3.3.html#section-6
@Last
@POST(":serverId<[^\\.]+>/:sessionId<[^\\.]+>/websocket")
class SockJSWebsocketPOST extends SockJsAction {
  def execute() {
    response.setStatus(HttpResponseStatus.METHOD_NOT_ALLOWED)
    response.setHeader(HttpHeaders.Names.ALLOW, "GET")
    respond()
  }
}

@WEBSOCKET(":serverId<[^\\.]+>/:sessionId<[^\\.]+>/websocket")
class SockJSWebsocket extends SockJsAction {
  def execute() {
    // Ignored
    //val sessionId = param("sessionId")

    val sockJsHandler = Routes.createSockJsHandler(pathPrefix)
    sockJsHandler.webSocketAction = this
    sockJsHandler.rawWebSocket    = false
    acceptWebSocket(new WebSocketHandler {
      def onOpen() {
        respondWebSocketText("o")
        sockJsHandler.onOpen(SockJSWebsocket.this)
      }

      def onClose() {
        sockJsHandler.onClose()
      }

      def onTextMessage(body: String) {
        // Server must ignore empty messages
        // http://sockjs.github.com/sockjs-protocol/sockjs-protocol-0.3.3.html#section-69
        if (body.isEmpty) return

        try {
          // body: can be ["m1", "m2"] or "m1"
          // http://sockjs.github.com/sockjs-protocol/sockjs-protocol-0.3.3.html#section-61
          val normalizedBody = if (body.startsWith("[")) body else "[" + body + "]"
          val messages       = Json.parse[Seq[String]](normalizedBody)
          messages.foreach(sockJsHandler.onMessage(_))
        } catch {
          case NonFatal(e) =>
            // No c frame is sent!
            // http://sockjs.github.com/sockjs-protocol/sockjs-protocol-0.3.3.html#section-72
            //respondWebSocket("c[2011,\"Broken JSON encoding.\"]")
            //.addListener(ChannelFutureListener.CLOSE)
            channel.close()
            sockJsHandler.onClose()
        }
      }

      def onBinaryMessage(binary: Array[Byte]) {
      }
    })
  }
}

@WEBSOCKET("websocket")
class SockJSRawWebsocket extends SockJsAction {
  def execute() {
    val immutableSession = session.toMap
    val sockJsHandler    = Routes.createSockJsHandler(pathPrefix)
    sockJsHandler.webSocketAction = this
    sockJsHandler.rawWebSocket    = true
    acceptWebSocket(new WebSocketHandler {
      def onOpen() {
        sockJsHandler.onOpen(SockJSRawWebsocket.this)
      }

      def onClose() {
        sockJsHandler.onClose()
      }

      def onTextMessage(text: String) {
        sockJsHandler.onMessage(text)
      }

      def onBinaryMessage(binary: Array[Byte]) {
      }
    })
  }
}
