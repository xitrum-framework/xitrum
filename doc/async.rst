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

Headers are only sent on the first renderXXX call.

Comet
-----

WebSocket and long polling Ajax

TODO
