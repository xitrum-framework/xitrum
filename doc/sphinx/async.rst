Async response
==============

.. image:: img/xitrum/lao_gamen_gargamel.jpg

respondXXX:

* ``respondInlineView``: responds HTML with or without layout
* ``respondView``: responds HTML with or without layout
* ``respondText``: responds a string without layout
* ``respondJson``: respondss JSON
* ``respondBinary``: responds an array of bytes
* ``respondFile``: sends a file directly from disk, very fast
  because `zero-copy <http://www.ibm.com/developerworks/library/j-zerocopy/>`_
  (aka send-file) is used

There is no default response. You must call respondXXX explicitly to send response
to the client. If you don't call respondXXX, the HTTP connection is kept for you,
and you can call respondXXX later.

Chunked response
----------------

`Chunked response <http://en.wikipedia.org/wiki/Chunked_transfer_encoding>`_
has many use cases. For example, when you need to generate a very large CSV
file that does may not fit memory.

::

  response.setChunked(true)

  val generator = new MyCsvGenerator
  val header = generator.getHeader
  respondText(header, "text/csv")

  while (generator.hasNextLine) {
    val line = generator.nextLine
    respondText(line)
  }

  respondLastChunk()

1. Call ``response.setChunked(true)``
2. Call respondXXX as many times as you want
3. Lastly, call ``respondLastChunk``

Notes:

* Headers are only sent on the first respondXXX call.
* Chunks cannot be used with :doc:`page or action cache </cache>`.

WebSocket
---------

Example:

::

  package quickstart.controller

  import xitrum.Controller

  class HelloWebSocket extends Controller {
    val index = WEBSOCKET("hello_websocket") {  // Entry point
      webSocketHandshake()  // Call this if you want to accept the WebSocket connection
    }

    override def onWebSocketFrame(text: String) {
      respondWebSocket(text.toUpperCase)  // Send back data to the WebSocket client
    }

    override def onWebSocketClose {
      println("The WebSocket connection has been closed")
    }
  }

Comet
-----

Comet messages may be clustered. Please see the chaper about :doc:`clustering </cluster>`.

Chunked response is `not very good <http://www.shanison.com/2010/05/10/stop-the-browser-%E2%80%9Cthrobber-of-doom%E2%80%9D-while-loading-comet-forever-iframe/>`_
for `Comet <http://en.wikipedia.org/wiki/Comet_(programming)/>`_.
Xitrum uses Ajax long polling. `WebSocket <http://en.wikipedia.org/wiki/WebSocket>`_
will be supported in the future when all major browsers support it.

Chat example
~~~~~~~~~~~~

::

  import xitrum.Controller
  import xitrum.comet.CometPublishController
  import xitrum.validator.{Required, Validated}

  class ChatController {
    val index = GET("chat") {
      jsCometGet("chat", """
        function(channel, timestamp, body) {
          var wasScrollAtBottom = xitrum.isScrollAtBottom('#chatOutput');
          $('#chatOutput').append('- ' + xitrum.escape(body.chatInput[0]) + '<br />');
          if (wasScrollAtBottom) xitrum.scrollToBottom('#chatOutput');
        }
      """)

      respondInlineView(
        <div id="chatOutput"></div>

        <form data-postback="submit" action={CometController.publish.url} data-after="$('#chatInput').value('')">
          <input type="hidden" name="channel" value="chat" class="required" />
          <input type="text" id="chatInput" name="chatInput" class="required" />
        </form>
      )
    }
  }

``jsCometGet`` will send long polling Ajax requests, get published messages,
and call your callback function. The 3rd argument ``body`` is a hash
containing everything inside the form commited to ``CometPublishController``.

Publish message
~~~~~~~~~~~~~~~

In the example above, ``CometPublishController`` will receive form post and publish
the message for you. If you want to publish the message yourself, call ``Comet.publish``:

::

  import xitrum.Controller
  import xitrum.comet.Comet

  class AdminController extends Controller {
    val index = GET("admin") {
      respondInlineView(
        <form data-postback="submit" action={publish.url}>
          <label>Message from admin:</label>
          <input type="text" name="body" class="required" />
        </form>
      )
    }

    val publish = POST("admin/chat") {
      val body = param("body")
      Comet.publish("chat", "[From admin]: " + body)
      respondText("")
    }
  }
