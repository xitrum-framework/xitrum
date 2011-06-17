Netty handlers
==============

This chapter is advanced. You must have knowlege about `Netty <http://netty.io/>`_.

`Rack <http://en.wikipedia.org/wiki/Rack_(Web_server_interface)>`_,
`WSGI <http://en.wikipedia.org/wiki/Web_Server_Gateway_Interface>`_, and
`PSGI <http://en.wikipedia.org/wiki/PSGI>`_ have middleware architecture.
You can create middleware and customize the order of middlewares.
Xitrum is based on `Netty <http://netty.io/>`_. Netty has the same
thing called handlers.

Xitrum lets you customize the channel pipeline of handlers. Doing this, you can
maximize server performance for your specific use case.

This chaper describes:

* Netty handler architecture
* Handlers that Xitrum provides and their default order
* How to create and use custom handler

Netty handler architecture
--------------------------

For each connection, there is a channel pipeline to handle the IO data.
A channel pipeline is a series of handlers.

In Netty, there are 2 types of handlers:
* upstream: the request direction client -> server
* downstream: the response direction server -> client

Please see the doc of `ChannelPipeline <http://netty.io/3.6/api/org/jboss/netty/channel/ChannelPipeline.html>`_
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

Custom handlers
---------------

When starting Xitrum server, you can pass in your own ChannelPipelineFactory:

::

  import xitrum.Server

  object Boot {
    def main(args: Array[String]) {
      Server.start(myChannelPipelineFactory)
    }
  }

For HTTPS server, Xitrum will automatically prepend SSL handler to the result of
``myChannelPipelineFactory.getPipeline``.

You can reuse Xitrum handlers in your pipeline.

Xitrum default handlers
-----------------------

See `xitrum.handler.DefaultHttpChannelPipelineFactory <https://github.com/ngocdaothanh/xitrum/blob/master/src/main/scala/xitrum/handler/ChannelPipelineFactory.scala>`_.

Sharable handlers (same instances are shared among many connections) are put in
``DefaultHttpChannelPipelineFactory`` object so that they can be easily picked
up by apps that want to use custom pipeline. Those apps may only want a subset
of default handlers.

When an app uses its own dispatcher (not Xitrum's routing/dispatcher) and
only needs Xitrum's fast static file serving, it may use only these handlers:

Upstream:

* HttpRequestDecoder
* noPipelining
* requestAttacher
* publicFileServer
* its own dispatcher

Downstream:

* HttpResponseEncoder
* ChunkedWriteHandler
* xSendFile

Tips
----

Channel attachment
~~~~~~~~~~~~~~~~~~

If you use Xitrum default handlers, HttpRequest is attached to the channel by
requestAttacher. Use Channel#getAttachment to get it back.

Channel close event
~~~~~~~~~~~~~~~~~~~

To act when the connection is closed, listen to the channel's close event:

::

  channel.getCloseFuture.addListener(new ChannelFutureListener {
    def operationComplete(future: ChannelFuture) {
      // Your code
    }
  })
