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

TODO

Comet
-----

WebSocket and long polling Ajax

TODO
