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

Below is a chat example:

Server side:

::

  import xitrum.Action  
  import xitrum.annotation.GET
  import xitrum.comet.Comet
  import xitrum.validation.Required

  @GET("/chat")
  class ChatIndexAction {
    override def execute {
      renderView(
        <div id="messages"></div>
        {jsAjaxGet(urlFor[ChatPollMessagesAction]("lastTimestamp" -> 0))}

        <input type="text" name={validate("message", Required)} postback="keypress" action={urlForPostbackThis} />
      )
    }

    override def postback {
      val message = param("message")
      Comet.publish("chat", message)
      renderText("")
    }
  }

::

  import xitrum.Action
  import xitrum.annotation.GET
  import xitrum.comet.Comet

  @GET("/chat/:lastTimestamp")
  class ChatPollMessagesAction extends Action {
    override def execute {
      val lastTimestamp = tparam[Long]("lastTimestamp")

      // messages: Iterable[xitrum.comet.CometMessage]
      val messages = Comet.getMessages("chat", lastTimestamp)

      if (messages.isEmpty) {
        Comet.addMessageListener("chat", message => {
          jsRender(
            "#('messages').append('" + <p><b>{message.timestamp}:</b> {message.body}</p> + "');" +
            jsAjaxGet(urlFor[ChatPollMessagesAction]("lastTimestamp" -> message.timestamp))
          )
          
          // Returns true to remove this listener
          true
        })
      } else {
          jsRender(
            messages.map("#('messages').append('" + <p><b>{message.timestamp}:</b> {message.body}</p> + "')").mkString(";") +
            jsAjaxGet(urlForThis("lastTimestamp" -> message.timestamp))
          )       
      }
    }
  }
