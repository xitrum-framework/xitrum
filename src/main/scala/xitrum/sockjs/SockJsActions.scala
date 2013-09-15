package xitrum.sockjs

import java.util.{Arrays, Random}
import scala.util.control.NonFatal

import org.jboss.netty.channel.{ChannelFuture, ChannelFutureListener}
import org.jboss.netty.buffer.ChannelBuffers
import org.jboss.netty.handler.codec.http.{DefaultCookie, HttpHeaders, HttpResponseStatus}

import akka.actor.{Actor, ActorRef, PoisonPill, Props, Terminated}

import glokka.Registry

import xitrum.{Action, ActionActor, Config, SkipCsrfCheck, SockJsText}
import xitrum.{WebSocketActor, WebSocketBinary, WebSocketPing, WebSocketPong, WebSocketText}
import xitrum.annotation._
import xitrum.etag.NotModified
import xitrum.scope.request.PathInfo
import xitrum.util.Json
import xitrum.view.DocType

// General info:
// http://sockjs.github.com/sockjs-protocol/sockjs-protocol-0.3.html
// http://en.wikipedia.org/wiki/Cross-origin_resource_sharing
// https://developer.mozilla.org/en-US/docs/HTTP_access_control
//
// Reference implementation (need to read when in doubt):
// https://github.com/sockjs/sockjs-node/tree/master/src
object SockJsAction {
  // All the chunking transports are closed by the server after 128K (production mode)
  // or 4K (development mode) was sent, in order to force client to GC and reconnect.
  // Last chunk is forcefully sent when limit is reached.
  val CHUNKED_RESPONSE_LIMIT = if (Config.productionMode) 128 * 1024 else 4 * 1024

  val actorRegistry = Registry.start(Config.actorSystem, getClass.getName)

  //----------------------------------------------------------------------------

  private[this] val random = new Random(System.currentTimeMillis())

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

  private[this] val htmlTemplateBefore = """<!doctype html>
<html><head>
  <meta http-equiv="X-UA-Compatible" content="IE=edge" />
  <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
</head><body><h2>Don't panic!</h2>
  <script>
    document.domain = document.domain;
    var c = parent."""

  private[this] val htmlTemplateAfter = """;
    c.start();
    function p(d) {c.message(d);};
    window.onload = function() {c.stop();};
  </script>"""

  // Nearly 1KB of spaces
  //
  // Safari needs at least 1024 bytes to parse the website:
  // http://code.google.com/p/browsersec/wiki/Part2#Survey_of_content_sniffing_behaviors
  //
  // https://github.com/sockjs/sockjs-node/blob/master/src/trans-htmlfile.coffee#L29
  // http://stackoverflow.com/questions/2804827/create-a-string-with-n-characters
  private[this] val s1KB = {
    val spaces = new Array[Char](1024 - htmlTemplateBefore.length + htmlTemplateAfter.length)
    Arrays.fill(spaces, ' ')
    new String(spaces) + "\r\n\r\n"
  }

  /** Template for htmlfile transport */
  def htmlfile(callback: String, with1024Spaces: Boolean): String = {
    val template = htmlTemplateBefore + callback + htmlTemplateAfter
    if (with1024Spaces) template + s1KB else template
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

trait SockJsPrefix {
  protected var pathPrefix = ""

  /** Called by Dispatcher. */
  def setPathPrefix(pathInfo: PathInfo) {
    val n       = nLastTokensToRemoveFromPathInfo
    val encoded = pathInfo.encoded
    pathPrefix =
      if (n == 0)
        encoded.substring(1)
      else if (n == 1)
        encoded.substring(1, encoded.lastIndexOf("/"))
      else {
        val tokens = pathInfo.tokens
        tokens.take(tokens.size - n).mkString("/")
      }
  }

  protected def nLastTokensToRemoveFromPathInfo: Int
}

trait SockJsAction extends Action with SockJsPrefix {
  // JSESSIONID cookie must be echoed back if sent by the client, or created
  // http://groups.google.com/group/sockjs/browse_thread/thread/71dfdff6e8f1e5f7
  // Can't use beforeFilter, see comment of pathPrefix at the top of this controller.
  protected def handleCookie() {
    val sockJsClassAndOptions = Config.routes.sockJsRouteMap.lookup(pathPrefix)
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
   * All the chunking transports are closed by the server after CHUNKED_RESPONSE_LIMIT
   * bytes was sent, in order to force client to GC and reconnect. The server doesn't have
   * to send "c" frame.
   *
   * @return false if the channel will be closed when the channel write completes
   */
  protected def respondStreamingWithLimit(text: String, isEventSource: Boolean = false): Boolean = {
    // This is length in characters, not bytes,
    // but in this case the result doesn't have to be precise
    val size = text.length
    streamingBytesSent += size
    if (streamingBytesSent < SockJsAction.CHUNKED_RESPONSE_LIMIT) {
      if (isEventSource) respondEventSource(text) else respondText(text)
      true
    } else {
      if (isEventSource) respondEventSource(text) else respondText(text)
      closeWithLastChunk()
      false
    }
  }

  protected def closeWithLastChunk() {
    respondLastChunk().addListener(ChannelFutureListener.CLOSE)
  }
}

trait SockJsNonWebSocketSessionActionActor extends ActionActor with SockJsAction {
  protected def lookupNonWebSocketSessionActor(sessionId: String) {
    SockJsAction.actorRegistry ! Registry.Lookup(sessionId)
  }
}

trait SockJsNonWebSocketSessionReceiverActionActor extends SockJsNonWebSocketSessionActionActor {
  protected var nonWebSocketSession: ActorRef = _

  // Call lookupOrCreateNonWebSocketSessionActor and continue with this method.
  // Here, context.become(receiveNotification) may be called.
  protected def onLookupOrRecreateResult(newlyCreated: Boolean)

  protected def receiveNotification: Receive

  protected def lookupOrCreateNonWebSocketSessionActor(sessionId: String) {
    // Try to lookup first, then create later
    SockJsAction.actorRegistry ! Registry.LookupOrCreate(sessionId)
    context.become({
      case Registry.LookupResultOk(`sessionId`, actorRef) =>
        nonWebSocketSession = actorRef
        context.watch(nonWebSocketSession)
        onLookupOrRecreateResult(false)

      case Registry.LookupResultNone(`sessionId`) =>
        // Must use context.system.actorOf instead of context.actorOf, so that
        // actorRef1 is not attached as a child to the current actor; otherwise
        // when the current actor dies, actorRef1 will be forcefully killed
        val action: Action = this
        val props          = Props(new NonWebSocketSession(Some(self), pathPrefix, action))
        val actorRef1      = context.system.actorOf(props)
        SockJsAction.actorRegistry ! Registry.Register(sessionId, actorRef1)

        context.become({
          case Registry.RegisterResultOk(`sessionId`, actorRef2) =>
            nonWebSocketSession = actorRef2
            context.watch(nonWebSocketSession)
            onLookupOrRecreateResult(true)

          case Registry.RegisterResultConflict(`sessionId`, actorRef2) =>
            context.system.stop(actorRef1)
            nonWebSocketSession = actorRef2
            context.watch(nonWebSocketSession)
            onLookupOrRecreateResult(false)
        })
    })
  }

  override def postStop() {
    if (nonWebSocketSession != null) {
      context.unwatch(nonWebSocketSession)
      if (!isDoneResponding) nonWebSocketSession ! AbortFromReceiverClient
    }
    super.postStop()
  }
}

@GET("")
class SockJsGreeting extends SockJsAction {
  def nLastTokensToRemoveFromPathInfo = 0

  def execute() {
    respondText("Welcome to SockJS!\n")
  }
}

@Last
@GET(":iframe")
class SockJsIframe extends SockJsAction {
  def nLastTokensToRemoveFromPathInfo = 1

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
  <script src={resourceUrl(src)}></script>
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
  def nLastTokensToRemoveFromPathInfo = 1

  def execute() {
    setCORS()
    setNoClientCache()

    val sockJsClassAndOptions = Config.routes.sockJsRouteMap.lookup(pathPrefix)
    respondJsonText(
      """{"websocket": """      + sockJsClassAndOptions.websocket +
      """, "cookie_needed": """ + sockJsClassAndOptions.cookieNeeded +
      """, "origins": ["*:*"], "entropy": """ + SockJsAction.entropy() + "}"
    )
  }
}

@OPTIONS("info")
class SockJsInfoOPTIONS extends SockJsAction {
  def nLastTokensToRemoveFromPathInfo = 1

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
  def nLastTokensToRemoveFromPathInfo = 3

  def execute() {
    xhrOPTIONS()
  }
}

@OPTIONS(":serverId<[^\\.]+>/:sessionId<[^\\.]+>/xhr_send")
class SockJsXhrPollingOPTIONSSend extends SockJsAction {
  def nLastTokensToRemoveFromPathInfo = 3

  def execute() {
    xhrOPTIONS()
  }
}

@POST(":serverId<[^\\.]+>/:sessionId<[^\\.]+>/xhr")
class SockJsXhrPollingReceive extends SockJsNonWebSocketSessionReceiverActionActor with SkipCsrfCheck {
  def nLastTokensToRemoveFromPathInfo = 3

  def execute() {
    val sessionId = param("sessionId")

    handleCookie()
    setCORS()
    setNoClientCache()

    lookupOrCreateNonWebSocketSessionActor(sessionId)
  }

  protected def onLookupOrRecreateResult(newlyCreated: Boolean) {
    if (newlyCreated) {
      respondJs("o\n")
    } else {
      nonWebSocketSession ! SubscribeFromReceiverClient
      context.become(receiveSubscribeResult)
    }
  }

  private def receiveSubscribeResult: Receive = {
    case SubscribeResultToReceiverClientAnotherConnectionStillOpen =>
      respondJs("c[2010,\"Another connection still open\"]\n")
      .addListener(ChannelFutureListener.CLOSE)

    case SubscribeResultToReceiverClientClosed =>
      respondJs("c[3000,\"Go away!\"]\n")
      .addListener(ChannelFutureListener.CLOSE)

    case SubscribeResultToReceiverClientMessages(messages) =>
      val json   = Json.generate(messages)
      val quoted = SockJsAction.quoteUnicode(json)
      respondJs("a" + quoted + "\n")

    case SubscribeResultToReceiverClientWaitForMessage =>
      context.become(receiveNotification)

    case Terminated(actorRef) if (actorRef == nonWebSocketSession) =>
      respondJs("c[2011,\"Server error\"]\n")
      .addListener(ChannelFutureListener.CLOSE)
  }

  protected override def receiveNotification: Receive = {
    case NotificationToReceiverClientMessage(message) =>
      val json   = Json.generate(Seq(message))
      val quoted = SockJsAction.quoteUnicode(json)
      respondJs("a" + quoted + "\n")

    case NotificationToReceiverClientHeartbeat =>
      respondJs("h\n")

    case NotificationToReceiverClientClosed =>
      respondJs("c[3000,\"Go away!\"]\n")
      .addListener(ChannelFutureListener.CLOSE)

    case Terminated(actorRef) if (actorRef == nonWebSocketSession) =>
      respondJs("c[2011,\"Server error\"]\n")
      .addListener(ChannelFutureListener.CLOSE)
  }
}

@POST(":serverId<[^\\.]+>/:sessionId<[^\\.]+>/xhr_send")
class SockJsXhrSend extends SockJsNonWebSocketSessionActionActor with SkipCsrfCheck {
  def nLastTokensToRemoveFromPathInfo = 3

  def execute() {
    val body = request.getContent.toString(Config.xitrum.request.charset)
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
      case Registry.LookupResultNone(`sessionId`) =>
        respondDefault404Page()

      case Registry.LookupResultOk(`sessionId`, nonWebSocketSession) =>
        nonWebSocketSession ! MessagesFromSenderClient(messages)
        response.setStatus(HttpResponseStatus.NO_CONTENT)
        response.setHeader(HttpHeaders.Names.CONTENT_TYPE, "text/plain; charset=UTF-8")
        setCORS()
        respond()
    }
  }
}

@OPTIONS(":serverId<[^\\.]+>/:sessionId<[^\\.]+>/xhr_streaming")
class SockJsXhrStreamingOPTIONSReceive extends SockJsAction {
  def nLastTokensToRemoveFromPathInfo = 3

  def execute() {
    xhrOPTIONS()
  }
}

@POST(":serverId<[^\\.]+>/:sessionId<[^\\.]+>/xhr_streaming")
class SockJsXhrStreamingReceive extends SockJsNonWebSocketSessionReceiverActionActor with SkipCsrfCheck {
  def nLastTokensToRemoveFromPathInfo = 3

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
  }

  protected def onLookupOrRecreateResult(newlyCreated: Boolean) {
    if (newlyCreated) {
      respondStreamingWithLimit("o\n")
      context.become(receiveNotification)
    } else {
      nonWebSocketSession ! SubscribeFromReceiverClient
      context.become(receiveSubscribeResult)
    }
  }

  private def receiveSubscribeResult: Receive = {
    case SubscribeResultToReceiverClientAnotherConnectionStillOpen =>
      respondJs("c[2010,\"Another connection still open\"]\n")
      closeWithLastChunk()

    case SubscribeResultToReceiverClientClosed =>
      respondJs("c[3000,\"Go away!\"]\n")
      closeWithLastChunk()

    case SubscribeResultToReceiverClientMessages(messages) =>
      val json   = Json.generate(messages)
      val quoted = SockJsAction.quoteUnicode(json)
      if (respondStreamingWithLimit("a" + quoted + "\n"))
        context.become(receiveNotification)

    case SubscribeResultToReceiverClientWaitForMessage =>
      context.become(receiveNotification)

    case Terminated(actorRef) if (actorRef == nonWebSocketSession) =>
      respondJs("c[2011,\"Server error\"]\n")
      closeWithLastChunk()
  }

  protected override def receiveNotification: Receive = {
    case NotificationToReceiverClientMessage(message) =>
      val json   = Json.generate(Seq(message))
      val quoted = SockJsAction.quoteUnicode(json)
      respondStreamingWithLimit("a" + quoted + "\n")

    case NotificationToReceiverClientHeartbeat =>
      respondStreamingWithLimit("h\n")

    case NotificationToReceiverClientClosed =>
      respondJs("c[3000,\"Go away!\"]\n")
      closeWithLastChunk()

    case Terminated(actorRef) if (actorRef == nonWebSocketSession) =>
      respondJs("c[2011,\"Server error\"]\n")
      closeWithLastChunk()
  }
}

@GET(":serverId<[^\\.]+>/:sessionId<[^\\.]+>/htmlfile")
class SockJshtmlfileReceive extends SockJsNonWebSocketSessionReceiverActionActor {
  def nLastTokensToRemoveFromPathInfo = 3

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
  }

  protected def onLookupOrRecreateResult(newlyCreated: Boolean) {
    if (newlyCreated) {
      response.setChunked(true)
      respondHtml(SockJsAction.htmlfile(callback, true))
      respondText("<script>\np(\"o\");\n</script>\r\n")
      context.become(receiveNotification)
    } else {
      nonWebSocketSession ! SubscribeFromReceiverClient
      context.become(receiveSubscribeResult)
    }
  }

  private def receiveSubscribeResult: Receive = {
    case SubscribeResultToReceiverClientAnotherConnectionStillOpen =>
      respondHtml(
        SockJsAction.htmlfile(callback, false) +
        "<script>\np(\"c[2010,\\\"Another connection still open\\\"]\");\n</script>\r\n"
      )
      .addListener(ChannelFutureListener.CLOSE)

    case SubscribeResultToReceiverClientClosed =>
      respondHtml(
        SockJsAction.htmlfile(callback, false) +
        "<script>\np(\"c[3000,\\\"Go away!\\\"]\");\n</script>\r\n"
      )
      .addListener(ChannelFutureListener.CLOSE)

    case SubscribeResultToReceiverClientMessages(messages) =>
      val buffer = new StringBuilder
      val json   = Json.generate(messages)
      val quoted = SockJsAction.quoteUnicode(json)
      buffer.append("<script>\np(\"a")
      buffer.append(jsEscape(quoted))
      buffer.append("\");\n</script>\r\n")
      response.setChunked(true)
      respondHtml(SockJsAction.htmlfile(callback, true))
      if (respondStreamingWithLimit(buffer.toString))
        context.become(receiveNotification)

    case SubscribeResultToReceiverClientWaitForMessage =>
      response.setChunked(true)
      respondHtml(SockJsAction.htmlfile(callback, true))
      context.become(receiveNotification)

    case Terminated(actorRef) if (actorRef == nonWebSocketSession) =>
      respondHtml(
        SockJsAction.htmlfile(callback, false) +
        "<script>\np(\"c[2011,\\\"Server error\\\"]\");\n</script>\r\n"
      )
      .addListener(ChannelFutureListener.CLOSE)
  }

  protected override def receiveNotification: Receive = {
    case NotificationToReceiverClientMessage(message) =>
      val buffer = new StringBuilder
      val json   = Json.generate(Seq(message))
      val quoted = SockJsAction.quoteUnicode(json)
      buffer.append("<script>\np(\"a")
      buffer.append(jsEscape(quoted))
      buffer.append("\");\n</script>\r\n")
      respondStreamingWithLimit(buffer.toString)

    case NotificationToReceiverClientHeartbeat =>
      respondStreamingWithLimit("<script>\np(\"h\");\n</script>\r\n")

    case NotificationToReceiverClientClosed =>
      respondHtml(
        SockJsAction.htmlfile(callback, false) +
        "<script>\np(\"c[3000,\\\"Go away!\\\"]\");\n</script>\r\n"
      )
      closeWithLastChunk()

    case Terminated(actorRef) if (actorRef == nonWebSocketSession) =>
      respondHtml(
        SockJsAction.htmlfile(callback, false) +
        "<script>\np(\"c[2011,\\\"Server error\\\"]\");\n</script>\r\n"
      )
      closeWithLastChunk()
  }
}

@GET(":serverId<[^\\.]+>/:sessionId<[^\\.]+>/jsonp")
class SockJsJsonPPollingReceive extends SockJsNonWebSocketSessionReceiverActionActor {
  def nLastTokensToRemoveFromPathInfo = 3

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
  }

  protected def onLookupOrRecreateResult(newlyCreated: Boolean) {
    if (newlyCreated) {
      respondJs(callback + "(\"o\");\r\n")
    } else {
      nonWebSocketSession ! SubscribeFromReceiverClient
      context.become(receiveSubscribeResult)
    }
  }

  private def receiveSubscribeResult: Receive = {
    case SubscribeResultToReceiverClientAnotherConnectionStillOpen =>
      respondJs(callback + "(\"c[2010,\\\"Another connection still open\\\"]\");\r\n")
      .addListener(ChannelFutureListener.CLOSE)

    case SubscribeResultToReceiverClientClosed =>
      respondJs(callback + "(\"c[3000,\\\"Go away!\\\"]\");\r\n")
      .addListener(ChannelFutureListener.CLOSE)

    case SubscribeResultToReceiverClientMessages(messages) =>
      val buffer = new StringBuilder
      val json   = Json.generate(messages)
      val quoted = SockJsAction.quoteUnicode(json)
      buffer.append(callback + "(\"a")
      buffer.append(jsEscape(quoted))
      buffer.append("\");\r\n")
      respondJs(buffer.toString)

    case SubscribeResultToReceiverClientWaitForMessage =>
      context.become(receiveNotification)

    case Terminated(actorRef) if (actorRef == nonWebSocketSession) =>
      respondJs(callback + "(\"c[2011,\\\"Server error\\\"]\");\r\n")
      .addListener(ChannelFutureListener.CLOSE)
  }

  protected override def receiveNotification: Receive = {
    case NotificationToReceiverClientMessage(message) =>
      val buffer = new StringBuilder
      val json   = Json.generate(Seq(message))
      val quoted = SockJsAction.quoteUnicode(json)
      buffer.append(callback + "(\"a")
      buffer.append(jsEscape(quoted))
      buffer.append("\");\r\n")
      respondJs(buffer.toString)

    case NotificationToReceiverClientHeartbeat =>
      respondJs(callback + "(\"h\");\r\n")

    case NotificationToReceiverClientClosed =>
      respondJs(callback + "(\"c[3000,\\\"Go away!\\\"]\");\r\n")
      .addListener(ChannelFutureListener.CLOSE)

    case Terminated(actorRef) if (actorRef == nonWebSocketSession) =>
      respondJs(callback + "(\"c[2011,\\\"Server error\\\"]\");\r\n")
      .addListener(ChannelFutureListener.CLOSE)
  }
}

@POST(":serverId<[^\\.]+>/:sessionId<[^\\.]+>/jsonp_send")
class SockJsJsonPPollingSend extends SockJsNonWebSocketSessionActionActor with SkipCsrfCheck {
  def nLastTokensToRemoveFromPathInfo = 3

  def execute() {
    val body: String = try {
      val contentType = request.getHeader(HttpHeaders.Names.CONTENT_TYPE)
      if (contentType != null && contentType.toLowerCase.startsWith(HttpHeaders.Values.APPLICATION_X_WWW_FORM_URLENCODED)) {
        param("d")
      } else {
        request.getContent.toString(Config.xitrum.request.charset)
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
      case Registry.LookupResultNone(`sessionId`) =>
        respondDefault404Page()

      case Registry.LookupResultOk(`sessionId`, nonWebSocketSession) =>
        nonWebSocketSession ! MessagesFromSenderClient(messages)
        // Konqueror does weird things on 204.
        // As a workaround we need to respond with something - let it be the string "ok".
        setCORS()
        setNoClientCache()
        respondText("ok")
    }
  }
}

@GET(":serverId<[^\\.]+>/:sessionId<[^\\.]+>/eventsource")
class SockJEventSourceReceive extends SockJsNonWebSocketSessionReceiverActionActor {
  def nLastTokensToRemoveFromPathInfo = 3

  def execute() {
    val sessionId = param("sessionId")

    handleCookie()
    setCORS()
    setNoClientCache()

    lookupOrCreateNonWebSocketSessionActor(sessionId)
  }

  protected def onLookupOrRecreateResult(newlyCreated: Boolean) {
    if (newlyCreated) {
      respondEventSource("o")
      context.become(receiveNotification)
    } else {
      nonWebSocketSession ! SubscribeFromReceiverClient
      context.become(receiveSubscribeResult)
    }
  }

  private def receiveSubscribeResult: Receive = {
    case SubscribeResultToReceiverClientAnotherConnectionStillOpen =>
      respondJs("c[2010,\"Another connection still open\"]\n")
      .addListener(ChannelFutureListener.CLOSE)

    case SubscribeResultToReceiverClientClosed =>
      respondJs("c[3000,\"Go away!\"]\n")
      .addListener(ChannelFutureListener.CLOSE)

    case SubscribeResultToReceiverClientMessages(messages) =>
      val json   = "a" + Json.generate(messages)
      val quoted = SockJsAction.quoteUnicode(json)
      if (respondStreamingWithLimit(quoted, true))
        context.become(receiveNotification)

    case SubscribeResultToReceiverClientWaitForMessage =>
      context.become(receiveNotification)

    case Terminated(actorRef) if (actorRef == nonWebSocketSession) =>
      respondJs("c[2011,\"Server error\"]\n")
      .addListener(ChannelFutureListener.CLOSE)
  }

  protected override def receiveNotification: Receive = {
    case NotificationToReceiverClientMessage(message) =>
      val json   = "a" + Json.generate(Seq(message))
      val quoted = SockJsAction.quoteUnicode(json)
      respondStreamingWithLimit(quoted, true)

    case NotificationToReceiverClientHeartbeat =>
      respondStreamingWithLimit("h", true)

    case NotificationToReceiverClientClosed =>
      respondJs("c[3000,\"Go away!\"]\n")
      closeWithLastChunk()

    case Terminated(actorRef) if (actorRef == nonWebSocketSession) =>
      respondEventSource("c[2011,\"Server error\"]")
      closeWithLastChunk()
  }
}

// http://sockjs.github.com/sockjs-protocol/sockjs-protocol-0.3.3.html#section-52
@Last
@GET(":serverId<[^\\.]+>/:sessionId<[^\\.]+>/websocket")
class SockJSWebsocketGET extends SockJsAction {
  def nLastTokensToRemoveFromPathInfo = 3

  def execute() {
    response.setStatus(HttpResponseStatus.BAD_REQUEST)
    respondText("""'Can "Upgrade" only to "WebSocket".'""")
  }
}

// http://sockjs.github.com/sockjs-protocol/sockjs-protocol-0.3.3.html#section-54
// http://sockjs.github.com/sockjs-protocol/sockjs-protocol-0.3.3.html#section-6
@Last
@POST(":serverId<[^\\.]+>/:sessionId<[^\\.]+>/websocket")
class SockJSWebsocketPOST extends SockJsAction with SkipCsrfCheck {
  def nLastTokensToRemoveFromPathInfo = 3

  def execute() {
    response.setStatus(HttpResponseStatus.METHOD_NOT_ALLOWED)
    response.setHeader(HttpHeaders.Names.ALLOW, "GET")
    respond()
  }
}

@WEBSOCKET(":serverId<[^\\.]+>/:sessionId<[^\\.]+>/websocket")
class SockJSWebsocket extends WebSocketActor with SockJsPrefix {
  def nLastTokensToRemoveFromPathInfo = 3

  private[this] var sockJsActorRef: ActorRef = _

  def execute() {
    // Ignored
    //val sessionId = param("sessionId")

    sockJsActorRef = Config.routes.sockJsRouteMap.createSockJsActor(pathPrefix)
    respondWebSocketText("o")
    sockJsActorRef ! (self, currentAction)

    context.become {
      case WebSocketText(body) =>
        // Server must ignore empty messages
        // http://sockjs.github.com/sockjs-protocol/sockjs-protocol-0.3.3.html#section-69
        if (!body.isEmpty) {
          try {
            // body: can be ["m1", "m2"] or "m1"
            // http://sockjs.github.com/sockjs-protocol/sockjs-protocol-0.3.3.html#section-61
            val normalizedBody = if (body.startsWith("[")) body else "[" + body + "]"
            val messages       = Json.parse[Seq[String]](normalizedBody)
            messages.foreach { msg => sockJsActorRef ! SockJsText(msg) }
          } catch {
            case NonFatal(e) =>
              // No c frame is sent!
              // http://sockjs.github.com/sockjs-protocol/sockjs-protocol-0.3.3.html#section-72
              //respondWebSocketText("c[2011,\"Broken JSON encoding.\"]")
              //.addListener(ChannelFutureListener.CLOSE)

              respondWebSocketClose()

//            action.channel.close()
          }
        }

      case MessageFromHandler(text) =>
        val json = Json.generate(Seq(text))
        respondWebSocketText("a" + json)

      case CloseFromHandler =>
        respondWebSocketText("c[3000,\"Go away!\"]").addListener(new ChannelFutureListener {
          def operationComplete(f: ChannelFuture) { respondWebSocketClose() }
        })

//        respondWebSocketClose()

//        respondWebSocketText("c[3000,\"Go away!\"]")
//        .addListener(ChannelFutureListener.CLOSE)

      case _ =>
        // Ignore all others
    }
  }

  override def postStop() {
    if (sockJsActorRef != null) Config.actorSystem.stop(sockJsActorRef)
    super.postStop()
  }
}

@WEBSOCKET("websocket")
class SockJSRawWebsocket extends WebSocketActor with SockJsPrefix {
  def nLastTokensToRemoveFromPathInfo = 1

  private[this] var sockJsActorRef: ActorRef = _

  def execute() {
    sockJsActorRef = Config.routes.sockJsRouteMap.createSockJsActor(pathPrefix)
    sockJsActorRef ! (self, currentAction)

    context.become {
      case WebSocketText(text) =>
        sockJsActorRef ! SockJsText(text)

      case MessageFromHandler(text) =>
        respondWebSocketText(text)

      case CloseFromHandler =>
        respondWebSocketClose()

      case _ =>
        // Ignore all others
    }
  }

  override def postStop() {
    if (sockJsActorRef != null) Config.actorSystem.stop(sockJsActorRef)
    super.postStop()
  }
}
