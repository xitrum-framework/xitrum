package xitrum.sockjs

import java.util.{Arrays, Random}

import org.jboss.netty.channel.{ChannelFuture, ChannelFutureListener}
import org.jboss.netty.buffer.ChannelBuffers
import org.jboss.netty.handler.codec.http.{HttpHeaders, HttpResponseStatus}

import com.codahale.jerkson.Json

import xitrum.{Config, Controller, SkipCSRFCheck}
import xitrum.etag.NotModified
import xitrum.routing.Routes
import xitrum.view.DocType

// General info:
// http://sockjs.github.com/sockjs-protocol/sockjs-protocol-0.3.html
// http://en.wikipedia.org/wiki/Cross-origin_resource_sharing
// https://developer.mozilla.org/en-US/docs/HTTP_access_control
//
// Reference implementation (need to read when in doubt):
// https://github.com/sockjs/sockjs-node/tree/master/src
object SockJsController {
  private val random = new Random(System.currentTimeMillis())

  /** 0 to 2^32 - 1 */
  def entropy() = random.nextInt().abs

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
}

class SockJsController extends Controller with SkipCSRFCheck {
  def greeting = GET("") {
    respondText("Welcome to SockJS!\n")
  }

  def iframe = last.GET(":iframe") {
    val iframe = param("iframe")
    if (iframe.startsWith("iframe") && iframe.endsWith(".html")) {
      val src =
        if (Config.isProductionMode)
          "xitrum/sockjs-0.3.min.js"
        else
          "xitrum/sockjs-0.3.js"

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

  def infoGET = GET("info") {
    setCORS()
    setNoClientCache()
    // FIXME: IE doesn't work with SockJS implementation of Xitrum when
    // cookie_needed is set to true
    respondJsonText("""{"websocket": true, "cookie_needed": false, "origins": ["*:*"], "entropy": """ + SockJsController.entropy() + "}")
  }

  def infoOPTIONS = OPTIONS("info") {
    response.setStatus(HttpResponseStatus.NO_CONTENT)
    response.setHeader("Access-Control-Allow-Methods", "OPTIONS, GET")
    setCORS()
    setClientCacheAggressively()
    respond()
  }

  //----------------------------------------------------------------------------

  def xhrPollingTransportOPTIONSReceive = OPTIONS(":serverId/:sessionId/xhr") {
    xhrTransportOPTIONS()
  }

  def xhrTransportOPTIONSSend = OPTIONS(":serverId/:sessionId/xhr_send") {
    xhrTransportOPTIONS()
  }

  def xhrPollingTransportReceive = POST(":serverId/:sessionId/xhr") {
    val sessionId = param("sessionId")

    SockJsPollingSessions.subscribeOnceByClient(pathPrefix, sessionId, { resulto =>
      setCORS()
      setNoClientCache()

      resulto match {
        case SubscribeByClientResultAnotherConnectionStillOpen =>
          respondJs("c[2010,\"Another connection still open\"]\n")

        case SubscribeByClientResultOpen =>
          respondJs("o\n")

        case SubscribeByClientResultMessages(messages) =>
          if (messages.isEmpty) {
            respondJs("h\n")
          } else {
            val json = "a" + Json.generate(messages) + "\n"
            respondJs(json)
          }

        case SubscribeByClientResultErrorAfterOpenHasBeenSent =>
          respondJs("c[2011,\"Server error\"]\n")
      }
    })
  }

  def xhrTransportSend = POST(":serverId/:sessionId/xhr_send") {
    val body = request.getContent().toString(Config.requestCharset)
    if (body.isEmpty) {
      response.setStatus(HttpResponseStatus.INTERNAL_SERVER_ERROR)
      respondText("Payload expected.")
    } else {
      val messages: List[String] = try {
        // body: ["m1", "m2"]
        Json.parse[List[String]](body)
      } catch {
        case _ =>
          response.setStatus(HttpResponseStatus.INTERNAL_SERVER_ERROR)
          respondText("Broken JSON encoding.")
          null
      }

      if (messages != null) {
        val sessionId = param("sessionId")
        if (SockJsPollingSessions.sendMessagesByClient(sessionId, messages)) {
          response.setStatus(HttpResponseStatus.NO_CONTENT)
          response.setHeader(HttpHeaders.Names.CONTENT_TYPE, "text/plain; charset=UTF-8")
          setCORS()
          respond()
        } else {
          respondDefault404Page()
        }
      }
    }
  }

  //----------------------------------------------------------------------------

  def xhrStreamingTransportOPTIONSReceive = OPTIONS(":serverId/:sessionId/xhr_streaming") {
    xhrTransportOPTIONS()
  }

  def xhrStreamingTransportReceive = POST(":serverId/:sessionId/xhr_streaming") {
    val sessionId = param("sessionId")

    // Below can be initiated by different channels, thus channel.isOpen should
    // be called to check if SockJsController.h2KB should be sent
    SockJsPollingSessions.subscribeStreamingByClient(pathPrefix, sessionId, { resulto =>
      resulto match {
        case SubscribeByClientResultAnotherConnectionStillOpen =>
          setCORS()
          setNoClientCache()
          respondJs("c[2010,\"Another connection still open\"]\n")
          false

        case SubscribeByClientResultOpen =>
          addConnectionClosedListener{ SockJsPollingSessions.unsubscribeByClient(sessionId) }
          setCORS()
          setNoClientCache()
          response.setChunked(true)
          response.setHeader(HttpHeaders.Names.CONTENT_TYPE, "application/javascript; charset=" + Config.config.request.charset)
          respondBinary(SockJsController.h2KB)
          respondStreamingWithLimit("o\n")
          true

        case SubscribeByClientResultMessages(messages) =>
          if (channel.isOpen()) {
            if (!isResponded) {
              addConnectionClosedListener{ SockJsPollingSessions.unsubscribeByClient(sessionId) }
              setCORS()
              setNoClientCache()
              response.setChunked(true)
              response.setHeader(HttpHeaders.Names.CONTENT_TYPE, "application/javascript; charset=" + Config.config.request.charset)
              respondBinary(SockJsController.h2KB)
            }

            if (messages.isEmpty) {
              respondStreamingWithLimit("h\n")
            } else {
              val json = "a" + Json.generate(messages) + "\n"
              respondStreamingWithLimit(json)
            }
          } else {
            false
          }

        case SubscribeByClientResultErrorAfterOpenHasBeenSent =>
          if (!isResponded) {
            setCORS()
            setNoClientCache()
            response.setHeader(HttpHeaders.Names.CONTENT_TYPE, "application/javascript; charset=" + Config.config.request.charset)
            respondJs("c[2011,\"Server error\"]\n")
          } else {
            respondJs("c[2011,\"Server error\"]\n")
            respondLastChunk()
          }
          false
      }
    })
  }

  //----------------------------------------------------------------------------

  def htmlfileTransportReceive = GET(":serverId/:sessionId/htmlfile") {
    val callbacko = callbackParam()
    if (callbacko.isDefined) {
      val callback  = callbacko.get
      val sessionId = param("sessionId")

      SockJsPollingSessions.subscribeStreamingByClient(pathPrefix, sessionId, { resulto =>
        resulto match {
          case SubscribeByClientResultAnotherConnectionStillOpen =>
            setCORS()
            setNoClientCache()
            respondHtml(
              SockJsController.htmlfile(callback, false) +
              "<script>\np(\"c[2010,\\\"Another connection still open\\\"]\");\n</script>\r\n"
            )
            false

          case SubscribeByClientResultOpen =>
            addConnectionClosedListener{ SockJsPollingSessions.unsubscribeByClient(sessionId) }
            setCORS()
            setNoClientCache()
            response.setChunked(true)
            respondHtml(SockJsController.htmlfile(callback, true))

            respondText("<script>\np(\"o\");\n</script>\r\n")
            true

          case SubscribeByClientResultMessages(messages) =>
            if (channel.isOpen()) {
              if (!isResponded) {
                addConnectionClosedListener{ SockJsPollingSessions.unsubscribeByClient(sessionId) }
                setCORS()
                setNoClientCache()
                response.setChunked(true)
                respondHtml(SockJsController.htmlfile(callback, true))
              }

              if (messages.isEmpty) {
                respondStreamingWithLimit("<script>\np(\"h\");\n</script>\r\n")
              } else {
                val buffer = new StringBuilder
                buffer.append("<script>\np(\"a")
                buffer.append(jsEscape(Json.generate(messages)))
                buffer.append("\");\n</script>\r\n")
                respondStreamingWithLimit(buffer.toString)
              }
            } else {
              false
            }

          case SubscribeByClientResultErrorAfterOpenHasBeenSent =>
            if (!isResponded) {
              addConnectionClosedListener{ SockJsPollingSessions.unsubscribeByClient(sessionId) }
              setCORS()
              setNoClientCache()
              respondHtml(
                SockJsController.htmlfile(callback, false) +
                "<script>\np(\"c[2011,\\\"Server error\\\"]\");\n</script>\r\n"
              )
            } else {
              respondText("<script>\np(\"c[2011,\\\"Server error\\\"]\");\n</script>\r\n")
              respondLastChunk()
            }
            false
        }
      })
    }
  }

  //----------------------------------------------------------------------------

  def jsonPPollingTransportReceive = GET(":serverId/:sessionId/jsonp") {
    val callbacko = callbackParam()
    if (callbacko.isDefined) {
      val callback  = callbacko.get
      val sessionId = param("sessionId")

      SockJsPollingSessions.subscribeOnceByClient(pathPrefix, sessionId, { resulto =>
        setCORS()
        setNoClientCache()

        resulto match {
          case SubscribeByClientResultAnotherConnectionStillOpen =>
            respondJs(callback + "(\"c[2010,\\\"Another connection still open\\\"]\");\r\n")

          case SubscribeByClientResultOpen =>
            respondJs(callback + "(\"o\");\r\n")

          case SubscribeByClientResultMessages(messages) =>
            if (messages.isEmpty) {
              respondJs(callback + "(\"h\");\r\n")
            } else {
              val buffer = new StringBuilder
              buffer.append(callback + "(\"a")
              buffer.append(jsEscape(Json.generate(messages)))
              buffer.append("\");\r\n")
              respondJs(buffer.toString)
            }

          case SubscribeByClientResultErrorAfterOpenHasBeenSent =>
            respondJs(callback + "(\"c[2011,\\\"Server error\\\"]\");\r\n")
        }
      })
    }
  }

  def jsonPTransportSend = POST(":serverId/:sessionId/jsonp_send") {
    val body: String = try {
      val contentType = request.getHeader(HttpHeaders.Names.CONTENT_TYPE)
      if (contentType != null && contentType.toLowerCase.startsWith(HttpHeaders.Values.APPLICATION_X_WWW_FORM_URLENCODED)) {
        param("d")
      } else {
        request.getContent().toString(Config.requestCharset)
      }
    } catch {
      case _ =>
        ""
    }

    if (body.isEmpty) {
      response.setStatus(HttpResponseStatus.INTERNAL_SERVER_ERROR)
      respondText("Payload expected.")
    } else {
      val sessionId = param("sessionId")

      val messages: List[String] = try {
        // body: ["m1", "m2"]
        Json.parse[List[String]](body)
      } catch {
        case _ =>
          response.setStatus(HttpResponseStatus.INTERNAL_SERVER_ERROR)
          respondText("Broken JSON encoding.")
          null
      }

      if (messages != null) {
        if (SockJsPollingSessions.sendMessagesByClient(sessionId, messages)) {
          // Konqueror does weird things on 204.
          // As a workaround we need to respond with something - let it be the string "ok".
          setCORS()
          setNoClientCache()
          respondText("ok")
        } else {
          respondDefault404Page()
        }
      }
    }
  }

  //----------------------------------------------------------------------------

  def eventSourceTransportReceive = GET(":serverId/:sessionId/eventsource") {
    val sessionId = param("sessionId")

    SockJsPollingSessions.subscribeStreamingByClient(pathPrefix, sessionId, { resulto =>
      resulto match {
        case SubscribeByClientResultAnotherConnectionStillOpen =>
          setCORS()
          setNoClientCache()
          respondJs("c[2010,\"Another connection still open\"]\n")
          false

        case SubscribeByClientResultOpen =>
          addConnectionClosedListener{ SockJsPollingSessions.unsubscribeByClient(sessionId) }
          setCORS()

          respondEventSource("o")
          true

        case SubscribeByClientResultMessages(messages) =>
          if (channel.isOpen()) {
            if (!isResponded) {
              addConnectionClosedListener{ SockJsPollingSessions.unsubscribeByClient(sessionId) }
              setCORS()
            }

            if (messages.isEmpty) {
              respondStreamingWithLimit("h", true)
            } else {
              val json = "a" + Json.generate(messages)
              respondStreamingWithLimit(json, true)
            }
          } else {
            false
          }

        case SubscribeByClientResultErrorAfterOpenHasBeenSent =>
          if (!isResponded) {
            setCORS()
            setNoClientCache()
            respondJs("c[2011,\"Server error\"]\n")
          } else {
            respondEventSource("c[2011,\"Server error\"]")
            respondLastChunk()
          }
          false
      }
    })
  }

  //----------------------------------------------------------------------------

  def websocketTransport = WEBSOCKET(":serverId/:sessionId/websocket") {
    // Ignored
    //val sessionId = param("sessionId")

    acceptWebSocket(new WebSocketHandler {
      val sockJsHandler = Routes.createSockJsHandler(pathPrefix)
      sockJsHandler.webSocketController = SockJsController.this
      sockJsHandler.rawWebSocket        = false

      def onOpen() {
        respondWebSocket("o")
        sockJsHandler.onOpen()
      }

      def onMessage(body: String) {
        val messages: List[String] = try {
          // body: ["m1", "m2"]
          Json.parse[List[String]](body)
        } catch {
          case _ =>
            val future = respondWebSocket("c[2011,\"Broken JSON encoding.\"]")
            future.addListener(new ChannelFutureListener {
              def operationComplete(f: ChannelFuture) { channel.close() }
            })
            null
        }
        if (messages != null) messages.foreach(sockJsHandler.onMessage(_))
      }

      def onClose() {
        sockJsHandler.onClose()
      }
    })
  }

  def rawWebsocketTransport = WEBSOCKET("websocket") {
    acceptWebSocket(new WebSocketHandler {
      val sockJsHandler = Routes.createSockJsHandler(pathPrefix)
      sockJsHandler.webSocketController = SockJsController.this
      sockJsHandler.rawWebSocket        = true

      def onOpen() {
        sockJsHandler.onOpen()
      }

      def onMessage(message: String) {
        sockJsHandler.onMessage(message)
      }

      def onClose() {
        sockJsHandler.onClose()
      }
    })
  }

  //----------------------------------------------------------------------------

  private def setCORS() {
    val requestOrigin  = request.getHeader(HttpHeaders.Names.ORIGIN)
    val responseOrigin = if (requestOrigin == null || requestOrigin == "null") "*" else requestOrigin
    response.setHeader("Access-Control-Allow-Origin",      responseOrigin)
    response.setHeader("Access-Control-Allow-Credentials", "true")

    val accessControlRequestHeaders = request.getHeader("Access-Control-Request-Headers")
    if (accessControlRequestHeaders != null)
      response.setHeader("Access-Control-Allow-Headers", accessControlRequestHeaders)
  }

  private def xhrTransportOPTIONS() {
    response.setStatus(HttpResponseStatus.NO_CONTENT)
    response.setHeader("Access-Control-Allow-Methods", "OPTIONS, POST")
    setCORS()
    setClientCacheAggressively()
    respond()
  }

  private def callbackParam(): Option[String] = {
    val paramName = if (uriParams.isDefinedAt("c")) "c" else "callback"
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
   * The test server should have this limit decreased to 4096B.
   */
  private var streamingBytesSent = 0

  private val LIMIT = if (Config.isProductionMode) 128 * 1024 else 4 * 1024

  /**
   * All the chunking transports are closed by the server after 128K was
   * send, in order to force client to GC and reconnect. The server doesn't have
   * to send "c" frame.
   * @return false if the channel will be closed when the channel write completes
   */
  private def respondStreamingWithLimit(text: String, isEventSource: Boolean = false): Boolean = {
    // This is length in characters, not bytes,
    // but in this case the result doesn't have to be precise
    val size = text.length
    streamingBytesSent += size
    if (streamingBytesSent < LIMIT) {
      if (isEventSource) respondEventSource(text) else respondText(text)
      true
    } else {
      val future = if (isEventSource) respondEventSource(text) else respondText(text)
      future.addListener(new ChannelFutureListener {
        def operationComplete(f: ChannelFuture) { channel.close() }
      })
      false
    }
  }
}
