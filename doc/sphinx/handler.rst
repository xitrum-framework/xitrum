Netty handlers
==============

This chapter is a little advanced, normally you don't need to read.

`Rack <http://en.wikipedia.org/wiki/Rack_(Web_server_interface)>`_,
`WSGI <http://en.wikipedia.org/wiki/Web_Server_Gateway_Interface>`_, and
`PSGI <http://en.wikipedia.org/wiki/PSGI>`_ have middleware architecture.
You can create middleware and customize the order of middlewares.
Xitrum is based on `Netty <http://www.jboss.org/netty>`_. Netty has the same
thing called handlers.

This chaper describes:

* Netty handler architecture
* Handlers that Xitrum provides and their default order
* How to create and use custom handler

Netty handler architecture
--------------------------

In Netty, there are 2 types of handlers:
* upstream: the request direction client -> server
* downstream: the response direction server -> client

Please see the doc of `ChannelPipeline <http://docs.jboss.org/netty/3.2/api/org/jboss/netty/channel/ChannelPipeline.html>`_
for more information.

::

                                       I/O Request
                                     via Channel or
                                 ChannelHandlerContext
                                           |
  +----------------------------------------+---------------+
  |                  ChannelPipeline       |               |
  |                                       \|/              |
  |  +----------------------+  +-----------+------------+  |
  |  | Upstream Handler  N  |  | Downstream Handler  1  |  |
  |  +----------+-----------+  +-----------+------------+  |
  |            /|\                         |               |
  |             |                         \|/              |
  |  +----------+-----------+  +-----------+------------+  |
  |  | Upstream Handler N-1 |  | Downstream Handler  2  |  |
  |  +----------+-----------+  +-----------+------------+  |
  |            /|\                         .               |
  |             .                          .               |
  |     [ sendUpstream() ]        [ sendDownstream() ]     |
  |     [ + INBOUND data ]        [ + OUTBOUND data  ]     |
  |             .                          .               |
  |             .                         \|/              |
  |  +----------+-----------+  +-----------+------------+  |
  |  | Upstream Handler  2  |  | Downstream Handler M-1 |  |
  |  +----------+-----------+  +-----------+------------+  |
  |            /|\                         |               |
  |             |                         \|/              |
  |  +----------+-----------+  +-----------+------------+  |
  |  | Upstream Handler  1  |  | Downstream Handler  M  |  |
  |  +----------+-----------+  +-----------+------------+  |
  |            /|\                         |               |
  +-------------+--------------------------+---------------+
                |                         \|/
  +-------------+--------------------------+---------------+
  |             |                          |               |
  |     [ Socket.read() ]          [ Socket.write() ]      |
  |                                                        |
  |  Netty Internal I/O Threads (Transport Implementation) |
  +--------------------------------------------------------+

Xitrum handlers
---------------

See `xitrum.handler.ChannelPipelineFactory <https://github.com/ngocdaothanh/xitrum/blob/master/src/main/scala/xitrum/handler/ChannelPipelineFactory.scala>`_.

Channel attachement
-------------------

HttpRequest is attached to the channel using Channel#setAttachment.
Use Channel#getAttachment to get it back.

Channel close event
-------------------

To act when the connection is closed, listen to the channel's close event: TODO

Custom handler
--------------

TODO: improve Xitrum to let user customize the order of handlers
