package xitrum.sockjs

import java.util.Random
import scala.concurrent.duration._
import scala.util.control.NonFatal

import io.netty.buffer.Unpooled
import io.netty.channel.{ChannelFuture, ChannelFutureListener}
import io.netty.handler.codec.http.{HttpHeaders, HttpResponseStatus}
import io.netty.handler.codec.http.cookie.DefaultCookie

import akka.actor.{ActorRef, Props, ReceiveTimeout, Terminated}
import glokka.Registry

import xitrum.{
  Action, ActorAction, Config, SkipCsrfCheck, SockJsText,
  WebSocketAction, WebSocketText
}
import xitrum.annotation._
import xitrum.scope.request.PathInfo
import xitrum.util.SeriDeseri
import xitrum.view.DocType

private object NotificationToHandlerUtil {
  def onComplete(
    channelFuture: ChannelFuture,
    index: Int, sockJsActorRef: ActorRef, write: Boolean
  ): ChannelFuture = {
    channelFuture.addListener(new ChannelFutureListener {
      def operationComplete(f: ChannelFuture) {
        val msg =
          if (write) {
            if (f.isSuccess)
              NotificationToHandlerChannelWriteSuccess(index)
            else
              NotificationToHandlerChannelWriteFailure(index)
          } else {
            if (f.isSuccess)
              NotificationToHandlerChannelCloseSuccess(index)
            else
              NotificationToHandlerChannelCloseFailure(index)
          }

        sockJsActorRef ! msg
      }
    })
    channelFuture
  }
}

// General info:
// http://sockjs.github.com/sockjs-protocol/sockjs-protocol-0.3.html
// http://en.wikipedia.org/wiki/Cross-origin_resource_sharing
// https://developer.mozilla.org/en-US/docs/HTTP_access_control
//
// Reference implementation (need to read when in doubt):
// https://github.com/sockjs/sockjs-node/tree/master/src
object SockJsAction {
  // The server must send a heartbeat frame every 25 seconds
  // http://sockjs.github.com/sockjs-protocol/sockjs-protocol-0.3.3.html#section-46
  val TIMEOUT_HEARTBEAT = 25.seconds

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

  /** 2K 'h' characters */
  val H2K = {
    val bytes   = Array.fill[Byte](2048 + 1)('h')
    bytes(2048) = '\n'
    bytes
  }

  private[this] val HTML_TEMPLATE_BEFORE = """<!doctype html>
<html><head>
  <meta http-equiv="X-UA-Compatible" content="IE=edge" />
  <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
</head><body><h2>Don't panic!</h2>
  <script>
    document.domain = document.domain;
    var c = parent."""

  private[this] val HTML_TEMPLATE_AFTER = """;
    c.start();
    function p(d) {c.message(d);};
    window.onload = function() {c.stop();};
  </script>"""

  // Nearly 1K spaces.
  //
  // Safari needs at least 1024 bytes to parse the website:
  // http://code.google.com/p/browsersec/wiki/Part2#Survey_of_content_sniffing_behaviors
  //
  // https://github.com/sockjs/sockjs-node/blob/master/src/trans-htmlfile.coffee#L29
  // http://stackoverflow.com/questions/2804827/create-a-string-with-n-characters
  private[this] val S1K = {
    val numSpaces = 1024 - HTML_TEMPLATE_BEFORE.length + HTML_TEMPLATE_AFTER.length
    val bytes     = Array.fill[Byte](numSpaces)(' ')
    new String(bytes) + "\r\n\r\n"
  }

  /** Template for htmlfile transport */
  def htmlFile(callback: String, with1KSpaces: Boolean): String = {
    val template = HTML_TEMPLATE_BEFORE + callback + HTML_TEMPLATE_AFTER
    if (with1KSpaces) template + S1K else template
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

trait ServerIdSessionIdValidator extends Action {
  // Server ID and session ID can't contain dots
  // (placeholder in URL can't be empty, no need to check)
  beforeFilter {
    if (pathParams.contains("serverId") || pathParams.contains("sessionId")) {
      val noDots = pathParams("serverId")(0).indexOf('.') < 0 && pathParams("sessionId")(0).indexOf('.') < 0
      if (!noDots) {
        response.setStatus(HttpResponseStatus.NOT_FOUND)
        respondText("")
      }
    }
  }
}

trait SockJsPrefix {
  var pathPrefix = ""

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
        tokens.take(tokens.length - n).mkString("/")
      }
  }

  protected def nLastTokensToRemoveFromPathInfo: Int
}

trait SockJsAction extends ServerIdSessionIdValidator with SockJsPrefix {
  // JSESSIONID cookie must be echoed back if sent by the client, or created
  // http://groups.google.com/group/sockjs/browse_thread/thread/71dfdff6e8f1e5f7
  // Can't use beforeFilter, see comment of pathPrefix at the top of this controller.
  protected def handleCookie() {
    val sockJsClassAndOptions = Config.routes.sockJsRouteMap.lookup(pathPrefix)
    if (sockJsClassAndOptions.cookieNeeded) {
      val value  = requestCookies.getOrElse("JSESSIONID", "dummy")
      val cookie = new DefaultCookie("JSESSIONID", value)
      responseCookies.append(cookie)
    }
  }

  protected def callbackParam(): Option[String] = {
    val paramName = if (handlerEnv.queryParams.isDefinedAt("c")) "c" else "callback"
    val ret = paramo(paramName)
    if (ret.isEmpty) {
      response.setStatus(HttpResponseStatus.INTERNAL_SERVER_ERROR)
      respondText("\"callback\" parameter required")
    }
    ret
  }
}

trait NonWebSocketSessionActorAction extends ActorAction with SockJsAction {
  protected def lookupNonWebSocketSessionActor(sessionId: String) {
    SockJsAction.actorRegistry ! Registry.Lookup(sessionId)
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
  protected def respondStreamingWithLimit(text: String, isEventSource: Boolean = false, index_handler: Option[(Int, ActorRef)] = None): Boolean = {
    // This is length in characters, not bytes,
    // but in this case the result doesn't have to be precise
    val size = text.length
    streamingBytesSent += size
    val (f, ret) = if (streamingBytesSent < SockJsAction.CHUNKED_RESPONSE_LIMIT) {
      val f = if (isEventSource) respondEventSource(text) else respondText(text)
      (f, true)
    } else {
      context.stop(self)
      val f = if (isEventSource) respondEventSource(text) else respondText(text)
      closeWithLastChunk()
      (f, false)
    }

    index_handler.foreach { case (index, handler) =>
      NotificationToHandlerUtil.onComplete(f, index, handler, false)
    }

    ret
  }

  protected def closeWithLastChunk(index_handler: Option[(Int, ActorRef)] = None) {
    val f = respondLastChunk().addListener(ChannelFutureListener.CLOSE)
    index_handler.foreach { case (index, handler) =>
      NotificationToHandlerUtil.onComplete(f, index, handler, false)
    }
  }
}

trait NonWebSocketSessionReceiverActorAction extends NonWebSocketSessionActorAction {
  protected var nonWebSocketSession: ActorRef = _

  // Call lookupOrCreateNonWebSocketSessionActor and continue with this method.
  // Here, context.become(receiveNotification) may be called.
  protected def onLookupOrRecreateResult(newlyCreated: Boolean)

  protected def receiveNotification: Receive

  protected def lookupOrCreateNonWebSocketSessionActor(sessionId: String) {
    // Try to lookup first, then create later
    val props = Props(new NonWebSocketSession(Some(self), pathPrefix, this))
    SockJsAction.actorRegistry ! Registry.Register(sessionId, props)
    context.become({
      case Registry.Found(`sessionId`, actorRef) =>
        nonWebSocketSession = actorRef
        context.watch(nonWebSocketSession)
        onLookupOrRecreateResult(false)

      case Registry.Created(`sessionId`, actorRef) =>
        nonWebSocketSession = actorRef
        context.watch(nonWebSocketSession)
        onLookupOrRecreateResult(true)
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
class Greeting extends SockJsAction {
  def nLastTokensToRemoveFromPathInfo = 0

  def execute() {
    respondText("Welcome to SockJS!\n")
  }
}

@Last
@GET(":iframe")
class Iframe extends SockJsAction {
  def nLastTokensToRemoveFromPathInfo = 1

  def execute() {
    val iframe = param("iframe")
    if (iframe.startsWith("iframe") && iframe.endsWith(".html")) {
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
  <script src={webJarsUrl("sockjs-client/1.0.3/dist", "sockjs-1.0.3.js", "sockjs-1.0.3.min.js")}></script>
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
class InfoGET extends SockJsAction {
  def nLastTokensToRemoveFromPathInfo = 1

  def execute() {
    setNoClientCache()

    val sockJsClassAndOptions = Config.routes.sockJsRouteMap.lookup(pathPrefix)
    respondJson(Map(
      "websocket"     -> sockJsClassAndOptions.websocket,
      "cookie_needed" -> sockJsClassAndOptions.cookieNeeded,
      "origins"       -> Config.xitrum.response.corsAllowOrigins,
      "entropy"       -> SockJsAction.entropy()
    ))
  }
}

@POST(":serverId/:sessionId/xhr")
class XhrPollingReceive extends NonWebSocketSessionReceiverActorAction with SkipCsrfCheck {
  def nLastTokensToRemoveFromPathInfo = 3

  def execute() {
    val sessionId = param("sessionId")

    handleCookie()
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
      val json   = SeriDeseri.toJson(messages)
      val quoted = SockJsAction.quoteUnicode(json)
      respondJs("a" + quoted + "\n")

    case SubscribeResultToReceiverClientWaitForMessage =>
      context.become(receiveNotification)

    case Terminated(actorRef) if actorRef == nonWebSocketSession =>
      respondJs("c[2011,\"Server error\"]\n")
      .addListener(ChannelFutureListener.CLOSE)
  }

  protected override def receiveNotification: Receive = {
    case NotificationToReceiverClientMessage(index, message, handler) =>
      val json   = SeriDeseri.toJson(Seq(message))
      val quoted = SockJsAction.quoteUnicode(json)
      NotificationToHandlerUtil.onComplete(respondJs("a" + quoted + "\n"), index, handler, true)

    case NotificationToReceiverClientHeartbeat =>
      respondJs("h\n")

    case NotificationToReceiverClientClosed(index, handler) =>
      NotificationToHandlerUtil.onComplete(
        respondJs("c[3000,\"Go away!\"]\n"),
        index, handler, false
      ).addListener(ChannelFutureListener.CLOSE)

    case Terminated(actorRef) if actorRef == nonWebSocketSession =>
      respondJs("c[2011,\"Server error\"]\n")
      .addListener(ChannelFutureListener.CLOSE)
  }
}

@POST(":serverId/:sessionId/xhr_send")
class XhrSend extends NonWebSocketSessionActorAction with SkipCsrfCheck {
  def nLastTokensToRemoveFromPathInfo = 3

  def execute() {
    val body = request.content.toString(Config.xitrum.request.charset)
    if (body.isEmpty) {
      response.setStatus(HttpResponseStatus.INTERNAL_SERVER_ERROR)
      respondText("Payload expected.")
      return
    }

    SeriDeseri.fromJson[Seq[String]](body) match {
      case None =>
        response.setStatus(HttpResponseStatus.INTERNAL_SERVER_ERROR)
        respondText("Broken JSON encoding.")

      case Some(messages) =>  // body: ["m1", "m2"]
        val sessionId = param("sessionId")
        lookupNonWebSocketSessionActor(sessionId)
        context.become {
          case Registry.NotFound(`sessionId`) =>
            respondDefault404Page()

          case Registry.Found(`sessionId`, nonWebSocketSession) =>
            nonWebSocketSession ! MessagesFromSenderClient(messages)
            response.setStatus(HttpResponseStatus.NO_CONTENT)
            HttpHeaders.setHeader(response, HttpHeaders.Names.CONTENT_TYPE, "text/plain; charset=UTF-8")
            respond()
        }
    }
  }
}

@POST(":serverId/:sessionId/xhr_streaming")
class XhrStreamingReceive extends NonWebSocketSessionReceiverActorAction with SkipCsrfCheck {
  def nLastTokensToRemoveFromPathInfo = 3

  def execute() {
    val sessionId = param("sessionId")

    handleCookie()
    setNoClientCache()

    // There's always 2KB prelude, even for immediate close frame
    HttpHeaders.setTransferEncodingChunked(response)
    HttpHeaders.setHeader(response, HttpHeaders.Names.CONTENT_TYPE, "application/javascript; charset=" + Config.xitrum.request.charset)
    respondBinary(Unpooled.wrappedBuffer(SockJsAction.H2K))

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
      val json   = SeriDeseri.toJson(messages)
      val quoted = SockJsAction.quoteUnicode(json)
      if (respondStreamingWithLimit("a" + quoted + "\n"))
        context.become(receiveNotification)

    case SubscribeResultToReceiverClientWaitForMessage =>
      context.become(receiveNotification)

    case Terminated(actorRef) if actorRef == nonWebSocketSession =>
      respondJs("c[2011,\"Server error\"]\n")
      closeWithLastChunk()
  }

  protected override def receiveNotification: Receive = {
    case NotificationToReceiverClientMessage(index, message, handler) =>
      val json   = SeriDeseri.toJson(Seq(message))
      val quoted = SockJsAction.quoteUnicode(json)
      respondStreamingWithLimit("a" + quoted + "\n", false, Some(index, handler))

    case NotificationToReceiverClientHeartbeat =>
      respondStreamingWithLimit("h\n")

    case NotificationToReceiverClientClosed(index, handler) =>
      respondJs("c[3000,\"Go away!\"]\n").addListener(new ChannelFutureListener {
        def operationComplete(f: ChannelFuture) {
          if (f.isSuccess)
            closeWithLastChunk(Some(index, handler))
          else
            NotificationToHandlerUtil.onComplete(f, index, handler, false)
        }
      })

    case Terminated(actorRef) if actorRef == nonWebSocketSession =>
      respondJs("c[2011,\"Server error\"]\n")
      closeWithLastChunk()
  }
}

@GET(":serverId/:sessionId/htmlfile")
class HtmlFileReceive extends NonWebSocketSessionReceiverActorAction {
  def nLastTokensToRemoveFromPathInfo = 3

  var callback: String = null

  def execute() {
    val callbacko = callbackParam()
    if (callbacko.isEmpty) return

    callback = callbacko.get
    val sessionId = param("sessionId")

    handleCookie()
    setNoClientCache()

    lookupOrCreateNonWebSocketSessionActor(sessionId)
  }

  protected def onLookupOrRecreateResult(newlyCreated: Boolean) {
    if (newlyCreated) {
      HttpHeaders.setTransferEncodingChunked(response)
      respondHtml(SockJsAction.htmlFile(callback, true))
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
        SockJsAction.htmlFile(callback, false) +
        "<script>\np(\"c[2010,\\\"Another connection still open\\\"]\");\n</script>\r\n"
      )
      .addListener(ChannelFutureListener.CLOSE)

    case SubscribeResultToReceiverClientClosed =>
      respondHtml(
        SockJsAction.htmlFile(callback, false) +
        "<script>\np(\"c[3000,\\\"Go away!\\\"]\");\n</script>\r\n"
      )
      .addListener(ChannelFutureListener.CLOSE)

    case SubscribeResultToReceiverClientMessages(messages) =>
      val buffer = new StringBuilder
      val json   = SeriDeseri.toJson(messages)
      val quoted = SockJsAction.quoteUnicode(json)
      buffer.append("<script>\np(\"a")
      buffer.append(jsEscape(quoted))
      buffer.append("\");\n</script>\r\n")
      HttpHeaders.setTransferEncodingChunked(response)
      respondHtml(SockJsAction.htmlFile(callback, true))
      if (respondStreamingWithLimit(buffer.toString))
        context.become(receiveNotification)

    case SubscribeResultToReceiverClientWaitForMessage =>
      HttpHeaders.setTransferEncodingChunked(response)
      respondHtml(SockJsAction.htmlFile(callback, true))
      context.become(receiveNotification)

    case Terminated(actorRef) if actorRef == nonWebSocketSession =>
      respondHtml(
        SockJsAction.htmlFile(callback, false) +
        "<script>\np(\"c[2011,\\\"Server error\\\"]\");\n</script>\r\n"
      )
      .addListener(ChannelFutureListener.CLOSE)
  }

  protected override def receiveNotification: Receive = {
    case NotificationToReceiverClientMessage(index, message, handler) =>
      val buffer = new StringBuilder
      val json   = SeriDeseri.toJson(Seq(message))
      val quoted = SockJsAction.quoteUnicode(json)
      buffer.append("<script>\np(\"a")
      buffer.append(jsEscape(quoted))
      buffer.append("\");\n</script>\r\n")
      respondStreamingWithLimit(buffer.toString, false, Some(index, handler))

    case NotificationToReceiverClientHeartbeat =>
      respondStreamingWithLimit("<script>\np(\"h\");\n</script>\r\n")

    case NotificationToReceiverClientClosed(index, handler) =>
      respondHtml(
        SockJsAction.htmlFile(callback, false) +
        "<script>\np(\"c[3000,\\\"Go away!\\\"]\");\n</script>\r\n"
      ).addListener(new ChannelFutureListener {
        def operationComplete(f: ChannelFuture) {
          if (f.isSuccess)
            closeWithLastChunk(Some(index, handler))
          else
            NotificationToHandlerUtil.onComplete(f, index, handler, false)
        }
      })

    case Terminated(actorRef) if actorRef == nonWebSocketSession =>
      respondHtml(
        SockJsAction.htmlFile(callback, false) +
        "<script>\np(\"c[2011,\\\"Server error\\\"]\");\n</script>\r\n"
      )
      closeWithLastChunk()
  }
}

@GET(":serverId/:sessionId/jsonp")
class JsonPPollingReceive extends NonWebSocketSessionReceiverActorAction {
  def nLastTokensToRemoveFromPathInfo = 3

  var callback: String = null

  def execute() {
    val callbacko = callbackParam()
    if (callbacko.isEmpty) return

    callback      = callbacko.get
    val sessionId = param("sessionId")

    handleCookie()
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
      val json   = SeriDeseri.toJson(messages)
      val quoted = SockJsAction.quoteUnicode(json)
      buffer.append(callback + "(\"a")
      buffer.append(jsEscape(quoted))
      buffer.append("\");\r\n")
      respondJs(buffer.toString)

    case SubscribeResultToReceiverClientWaitForMessage =>
      context.become(receiveNotification)

    case Terminated(actorRef) if actorRef == nonWebSocketSession =>
      respondJs(callback + "(\"c[2011,\\\"Server error\\\"]\");\r\n")
      .addListener(ChannelFutureListener.CLOSE)
  }

  protected override def receiveNotification: Receive = {
    case NotificationToReceiverClientMessage(index, message, handler) =>
      val buffer = new StringBuilder
      val json   = SeriDeseri.toJson(Seq(message))
      val quoted = SockJsAction.quoteUnicode(json)
      buffer.append(callback + "(\"a")
      buffer.append(jsEscape(quoted))
      buffer.append("\");\r\n")
      NotificationToHandlerUtil.onComplete(respondJs(buffer.toString), index, handler, true)

    case NotificationToReceiverClientHeartbeat =>
      respondJs(callback + "(\"h\");\r\n")

    case NotificationToReceiverClientClosed(index, handler) =>
      NotificationToHandlerUtil.onComplete(
        respondJs(callback + "(\"c[3000,\\\"Go away!\\\"]\");\r\n"),
        index, handler, false
      ).addListener(ChannelFutureListener.CLOSE)

    case Terminated(actorRef) if actorRef == nonWebSocketSession =>
      respondJs(callback + "(\"c[2011,\\\"Server error\\\"]\");\r\n")
      .addListener(ChannelFutureListener.CLOSE)
  }
}

@POST(":serverId/:sessionId/jsonp_send")
class JsonPPollingSend extends NonWebSocketSessionActorAction with SkipCsrfCheck {
  def nLastTokensToRemoveFromPathInfo = 3

  def execute() {
    val body: String = try {
      val contentType = HttpHeaders.getHeader(request, HttpHeaders.Names.CONTENT_TYPE)
      if (contentType != null && contentType.toLowerCase.startsWith(HttpHeaders.Values.APPLICATION_X_WWW_FORM_URLENCODED)) {
        param("d")
      } else {
        request.content.toString(Config.xitrum.request.charset)
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

    SeriDeseri.fromJson[Seq[String]](body) match {
      case None =>
        response.setStatus(HttpResponseStatus.INTERNAL_SERVER_ERROR)
        respondText("Broken JSON encoding.")

      case Some(messages) =>  // body: ["m1", "m2"]
        val sessionId = param("sessionId")
        lookupNonWebSocketSessionActor(sessionId)
        context.become {
          case Registry.NotFound(`sessionId`) =>
            respondDefault404Page()

          case Registry.Found(`sessionId`, nonWebSocketSession) =>
            nonWebSocketSession ! MessagesFromSenderClient(messages)
            // Konqueror does weird things on 204.
            // As a workaround we need to respond with something - let it be the string "ok".
            setNoClientCache()
            respondText("ok")
        }
    }
  }
}

@GET(":serverId/:sessionId/eventsource")
class EventSourceReceive extends NonWebSocketSessionReceiverActorAction {
  def nLastTokensToRemoveFromPathInfo = 3

  def execute() {
    val sessionId = param("sessionId")

    handleCookie()
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
      val json   = "a" + SeriDeseri.toJson(messages)
      val quoted = SockJsAction.quoteUnicode(json)
      if (respondStreamingWithLimit(quoted, true))
        context.become(receiveNotification)

    case SubscribeResultToReceiverClientWaitForMessage =>
      context.become(receiveNotification)

    case Terminated(actorRef) if actorRef == nonWebSocketSession =>
      respondJs("c[2011,\"Server error\"]\n")
      .addListener(ChannelFutureListener.CLOSE)
  }

  protected override def receiveNotification: Receive = {
    case NotificationToReceiverClientMessage(index, message, handler) =>
      val json   = "a" + SeriDeseri.toJson(Seq(message))
      val quoted = SockJsAction.quoteUnicode(json)
      respondStreamingWithLimit(quoted, true, Some(index, handler))

    case NotificationToReceiverClientHeartbeat =>
      respondStreamingWithLimit("h", true)

    case NotificationToReceiverClientClosed(index, handler) =>
      respondJs("c[3000,\"Go away!\"]\n")
      closeWithLastChunk(Some(index, handler))

    case Terminated(actorRef) if actorRef == nonWebSocketSession =>
      respondEventSource("c[2011,\"Server error\"]")
      closeWithLastChunk()
  }
}

// http://sockjs.github.com/sockjs-protocol/sockjs-protocol-0.3.3.html#section-52
@Last
@GET(":serverId/:sessionId/websocket")
class WebSocketGET extends SockJsAction {
  def nLastTokensToRemoveFromPathInfo = 3

  def execute() {
    response.setStatus(HttpResponseStatus.BAD_REQUEST)
    respondText("""'Can "Upgrade" only to "WebSocket".'""")
  }
}

// http://sockjs.github.com/sockjs-protocol/sockjs-protocol-0.3.3.html#section-54
// http://sockjs.github.com/sockjs-protocol/sockjs-protocol-0.3.3.html#section-6
@Last
@POST(":serverId/:sessionId/websocket")
class WebSocketPOST extends SockJsAction with SkipCsrfCheck {
  def nLastTokensToRemoveFromPathInfo = 3

  def execute() {
    response.setStatus(HttpResponseStatus.METHOD_NOT_ALLOWED)
    HttpHeaders.setHeader(response, HttpHeaders.Names.ALLOW, "GET")
    respond()
  }
}

// sessionId is ignored
@WEBSOCKET(":serverId/:sessionId/websocket")
class WebSocket extends WebSocketAction with ServerIdSessionIdValidator with SockJsPrefix {
  def nLastTokensToRemoveFromPathInfo = 3

  private[this] var sockJsActorRef: ActorRef = _

  def execute() {
    sockJsActorRef = Config.routes.sockJsRouteMap.createSockJsAction(pathPrefix)
    respondWebSocketText("o")
    sockJsActorRef ! (self, currentAction)

    context.setReceiveTimeout(SockJsAction.TIMEOUT_HEARTBEAT)
    context.become {
      case ReceiveTimeout =>
        respondWebSocketText("h")

      case WebSocketText(body) =>
        // Server must ignore empty messages
        // http://sockjs.github.com/sockjs-protocol/sockjs-protocol-0.3.3.html#section-69
        if (!body.isEmpty) {
          // body: can be ["m1", "m2"] or "m1"
          // http://sockjs.github.com/sockjs-protocol/sockjs-protocol-0.3.3.html#section-61
          val normalizedBody = if (body.startsWith("[")) body else "[" + body + "]"
          SeriDeseri.fromJson[Seq[String]](normalizedBody) match {
            case None =>
              // No c frame is sent!
              // http://sockjs.github.com/sockjs-protocol/sockjs-protocol-0.3.3.html#section-72
              //respondWebSocketText("c[2011,\"Broken JSON encoding.\"]")
              //.addListener(ChannelFutureListener.CLOSE)

              respondWebSocketClose()

            case Some(messages) =>
              messages.foreach { msg => sockJsActorRef ! SockJsText(msg) }
          }
        }

      case MessageFromHandler(index, text) =>
        val json = SeriDeseri.toJson(Seq(text))
        NotificationToHandlerUtil.onComplete(respondWebSocketText("a" + json), index, sockJsActorRef, true)

      case CloseFromHandler(index) =>
        respondWebSocketText("c[3000,\"Go away!\"]").addListener(new ChannelFutureListener {
          def operationComplete(f: ChannelFuture) {
            if (f.isSuccess)
              NotificationToHandlerUtil.onComplete(respondWebSocketClose(), index, sockJsActorRef, false)
            else
              NotificationToHandlerUtil.onComplete(f, index, sockJsActorRef, false)
          }
        })
    }
  }

  override def postStop() {
    if (sockJsActorRef != null) Config.actorSystem.stop(sockJsActorRef)
    super.postStop()
  }
}

@WEBSOCKET("websocket")
class RawWebSocket extends WebSocketAction with ServerIdSessionIdValidator with SockJsPrefix {
  def nLastTokensToRemoveFromPathInfo = 1

  private[this] var sockJsActorRef: ActorRef = _

  def execute() {
    sockJsActorRef = Config.routes.sockJsRouteMap.createSockJsAction(pathPrefix)
    sockJsActorRef ! (self, currentAction)

    context.become {
      case WebSocketText(text) =>
        sockJsActorRef ! SockJsText(text)

      case MessageFromHandler(index, text) =>
        NotificationToHandlerUtil.onComplete(respondWebSocketText(text), index, sockJsActorRef, true)

      case CloseFromHandler(index) =>
        NotificationToHandlerUtil.onComplete(respondWebSocketClose(), index, sockJsActorRef, false)
    }
  }

  override def postStop() {
    if (sockJsActorRef != null) Config.actorSystem.stop(sockJsActorRef)
    super.postStop()
  }
}
