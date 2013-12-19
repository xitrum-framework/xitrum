Async response
==============

List of normal responding methods:

* ``respondView``: responds view template with or without layout
* ``respondInlineView``: responds with or without layout
* ``respondText("hello")``: responds a string without layout
* ``respondHtml("<html>...</html>")``: same as above, with content type set to "text/html"
* ``respondJson(List(1, 2, 3))``: converts Scala object to JSON object then responds
* ``respondJs("myFunction([1, 2, 3])")``
* ``respondJsonP(List(1, 2, 3), "myFunction")``: combination of the above two
* ``respondJsonText("[1, 2, 3]")``
* ``respondJsonPText("[1, 2, 3]", "myFunction")``
* ``respondBinary``: responds an array of bytes
* ``respondFile``: sends a file directly from disk, very fast
  because `zero-copy <http://www.ibm.com/developerworks/library/j-zerocopy/>`_
  (aka send-file) is used
* ``respondEventSource("data", "event")``

Xitrum does not automatically send any default response.
You must explicitly call respondXXX methods above to send response.
If you don't call respondXXX, Xitrum will keep the HTTP connection for you,
and you can call respondXXX later.

To check if the connection is still open, call ``channel.isOpen``.
You can also use ``addConnectionClosedListener``:

::

  addConnectionClosedListener {
    // The connection has been closed
    // Unsubscribe from events, release resources etc.
  }

Because of the async nature, the response is not sent right away.
respondXXX returns
`ChannelFuture <http://netty.io/3.8/api/org/jboss/netty/channel/ChannelFuture.html>`_.
You can use it to perform actions when the response has actually been sent.

For example, if you want to close the connection after the response has been sent:

::

  import org.jboss.netty.channel.{ChannelFuture, ChannelFutureListener}

  val future = respondText("Hello")
  future.addListener(new ChannelFutureListener {
    def operationComplete(future: ChannelFuture) {
      future.getChannel.close()
    }
  })

Or shorter:

::

  respondText("Hello").addListener(ChannelFutureListener.CLOSE)

WebSocket
---------

::

  import xitrum.WebSocketActor
  import xitrum.annotation.WEBSOCKET

  @WEBSOCKET("echo")
  class EchoWebSocketActor extends WebSocketActor {
    /**
     * The current action is the one just before switching to this WebSocket actor.
     * You can extract session data, request headers etc. from it, but do not use
     * respondText, respondView etc.
     */
    def execute() {
      log..debug("onOpen")

      context.become {
        case WebSocketText(text) =>
          log.info("onTextMessage: " + text)
          respondWebSocketText(text.toUpperCase)

        case WebSocketBinary(bytes) {
          log.info("onBinaryMessage: " + bytes)
          respondWebSocketBinary(bytes)

        case WebSocketPing =>
          log.debug("onPing")

        case WebSocketPong =>
          log.debug("onPong")
      }
    }

    override def postStop() {
      log.debug("onClose")
      super.postStop()
    }
  }

An actor will be created when there's request. It will be stopped when:

* The connection is closed
* WebSocket close frame is received or sent

Use these to send WebSocket frames:

* respondWebSocketText
* respondWebSocketBinary
* respondWebSocketPing
* respondWebSocketClose

There's no respondWebSocketPong, because pong is automatically sent by Xitrum for you.

To get URL to the above WebSocket action:

::

  // Probably you want to use this in Scalate view etc.
  val url = webSocketAbsUrl[EchoWebSocketActor]

SockJS
------

`SockJS <https://github.com/sockjs/sockjs-client>`_ is a browser JavaScript
library that provides a WebSocket-like object.
SockJS tries to use WebSocket first. If that fails it can use a variety
of ways but still presents them through the WebSocket-like object.

If you want to work with WebSocket API on all kind of browsers, you should use
SockJS and avoid using WebSocket directly.

::

  <script>
    var sock = new SockJS('http://mydomain.com/path_prefix');
    sock.onopen = function() {
      console.log('open');
    };
    sock.onmessage = function(e) {
      console.log('message', e.data);
    };
    sock.onclose = function() {
      console.log('close');
    };
  </script>

Xitrum includes the JavaScript file of SockJS.
In your view template, just write like this:

::

  ...
  html
    head
      != jsDefaults
  ...

SockJS does require a `server counterpart <https://github.com/sockjs/sockjs-protocol>`_.
Xitrum automatically does it for you.

::

  import xitrum.{Action, SockJsActor, SockJsText}
  import xitrum.annotation.SOCKJS

  @SOCKJS("echo")
  class EchoSockJsActor extends SockJsActor {
    /**
     * The current action is the one just before switching to this SockJS actor.
     * You can extract session data, request headers etc. from it, but do not use
     * respondText, respondView etc.
     */
    def execute() {
      log.info("onOpen")

      context.become {
        case SockJsText(text) =>
          log.info("onMessage: " + text)
          respondSockJsText(text)
      }
    }

    override def postStop() {
      log.info("onClose")
      super.postStop()
    }
  }

An actor will be created when there's new SockJS session. It will be stopped when
the SockJS session is closed.

Use these to send SockJS frames:

* respondSockJsText
* respondSockJsClose

See `Various issues and design considerations <https://github.com/sockjs/sockjs-node#various-issues-and-design-considerations>`_:

::

  Basically cookies are not suited for SockJS model. If you want to authorize a
  session, provide a unique token on a page, send it as a first thing over SockJS
  connection and validate it on the server side. In essence, this is how cookies
  work.

To config SockJS clustering, see :doc:`Clustering with Akka </cluster>`.

Chunked response
----------------

1. Call ``response.setChunked(true)``
2. Call respondXXX as many times as you want
3. Lastly, call ``respondLastChunk``

`Chunked response <http://en.wikipedia.org/wiki/Chunked_transfer_encoding>`_
has many use cases. For example, when you need to generate a very large CSV
file that does may not fit memory.

::

  // "Cache-Control" header will be automatically set to:
  // "no-store, no-cache, must-revalidate, max-age=0"
  // Note that "Pragma: no-cache" is linked to requests, not responses:
  // http://palizine.plynt.com/issues/2008Jul/cache-control-attributes/
  response.setChunked(true)

  val generator = new MyCsvGenerator
  val header = generator.getHeader
  respondText(header, "text/csv")

  while (generator.hasNextLine) {
    val line = generator.nextLine
    respondText(line)
  }

  respondLastChunk()

Notes:

* Headers are only sent on the first respondXXX call.
* :doc:`Page and action cache </cache>` cannot be used with chunked response.

Using chunked response together with ActionActor, you can easily implement
`Facebook BigPipe <http://www.cubrid.org/blog/dev-platform/faster-web-page-loading-with-facebook-bigpipe/>`_.

Forever iframe
~~~~~~~~~~~~~~

Chunked response `can be used <http://www.shanison.com/2010/05/10/stop-the-browser-%E2%80%9Cthrobber-of-doom%E2%80%9D-while-loading-comet-forever-iframe/>`_
for `Comet <http://en.wikipedia.org/wiki/Comet_(programming)/>`_.

The page that embeds the iframe:

::

  ...
  <script>
    var functionForForeverIframeSnippetsToCall = function() {...}
  </script>
  ...
  <iframe width="1" height="1" src="path/to/forever/iframe"></iframe>
  ...

The action that responds <script> snippets forever:

::

  response.setChunked(true)

  // Need something like "123" for Firefox to work
  respondText("<html><body>123", "text/html")

  // Most clients (even curl!) do not execute <script> snippets right away,
  // we need to send about 2KB dummy data to bypass this problem
  for (i <- 1 to 100) respondText("<script></script>\n")

Later, whenever you want to pass data to the browser, just send a snippet:

::

  if (channel.isOpen)
    respondText("<script>parent.functionForForeverIframeSnippetsToCall()</script>\n")
  else
    // The connection has been closed, unsubscribe from events etc.
    // You can also use ``addConnectionClosedListener``.

Event Source
~~~~~~~~~~~~

See http://dev.w3.org/html5/eventsource/

Event Source response is a special kind of chunked response.
Data must be Must be  UTF-8.

To respond event source, call ``respondEventSource`` as many time as you want.

::

  respondEventSource("data1", "event1")
  respondEventSource("data2")  // Event name defaults to "message"
