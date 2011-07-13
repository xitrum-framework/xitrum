Async response
==============

.. image:: img/lao_gamen_gargamel.jpg

renderXXX:

* ``renderView``: render HTML with or without layout
* ``renderText``: render a string without layout
* ``renderBinary``: render an array of bytes
* ``renderFile``: send a file directly from disk, very fast
  because `zero-copy <http://www.ibm.com/developerworks/library/j-zerocopy/>`_
  (send-file) is used

There is no default response. You must call renderXXX explicitly to send response
to the client. If you don't call renderXXX, the HTTP connection is kept for you,
and you can call renderXXX later.

Chunked response
----------------

`Chunked response <http://en.wikipedia.org/wiki/Chunked_transfer_encoding>`_
has many use cases. For example, when you need to generate a very large CSV
file that does may not fit memory.

::

  response.setChunked(true)

  val generator = new MyCsvGenerator
  val header = generator.getHeader
  renderText(header, "text/csv")

  while (generator.hasNextLine) {
    val line = generator.nextLine
    renderText(line)
  }

  renderLastChunk

1. Call ``response.setChunked(true)``
2. Call renderXXX as many times as you want
3. Lastly, call ``renderLastChunk``

Notes:

* Headers are only sent on the first renderXXX call.
* Chunks cannot be used with :doc:`page or action cache </cache>`.

Comet
-----

Comet messages may be clustered. Please see the chaper about :doc:`clustering </cluster>`.

Chunked response is `not very good <http://www.shanison.com/2010/05/10/stop-the-browser-%E2%80%9Cthrobber-of-doom%E2%80%9D-while-loading-comet-forever-iframe/>`_
for `Comet <http://en.wikipedia.org/wiki/Comet_(programming)/>`_.

Xitrum uses Ajax long polling. `WebSocket <http://en.wikipedia.org/wiki/WebSocket>`
will be supported in the future when all major browsers support it.

Chat example
~~~~~~~~~~~~

::

  import xitrum.Action
  import xitrum.annotation.GET
  import xitrum.comet.CometPublishAction
  import xitrum.validation.Required

  @GET("/chat")
  class ChatAction {
    override def execute {
      renderView(
        <div id="messages"></div>
        {jsCometGet("chat", "function(message) { #('messages').append(message.body) }"}

        <form postback="submit" action={urlForPostback[CometPublishAction]}>
          <input type="hidden" name={validate("channel")} value="chat" />
          <input type="text" name={validate("body", Required)} />
        </form>
      )
    }
  }

``jsCometGet`` will send long polling Ajax requests, get published messages,
and call your defined function.

Publish message
~~~~~~~~~~~~~~~

In the example above, ``CometPublishAction`` will receive form post and publish
the message for you. If you want to publish the message yourself, call ``Comet.publish``:

::

  import xitrum.Action
  import xitrum.annotation.GET
  import xitrum.comet.Comet
  import xitrum.validation.Required

  @GET("/admin")
  class AdminAction extends Action {
    override def execute {
      renderView(
        <form postback="submit" action={urlForPostbackThis}>
          Message from admin:
          <input type="text" name={validate("body", Required)} />
        </form>
      )
    }

    override def postback {
      val body = param("body")
      Comet.publish("chat", "[From admin]: " + body)
      renderText("")
    }
  }
