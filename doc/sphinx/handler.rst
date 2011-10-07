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

::

                                                                                                                           /favicon.ico
                                                                                                                           /robots.txt                                                                                       fileParams
                                                        Request                                                            /public/...           /resources/public/...                 Env            uriParams              bodyParams                                pathParams
  [upstream]  HttpRequestDecoder -> HttpChunkAggregator -------------------------------------------------> | XSendfile| -> PublicFileServer -+-> PublicResourceServer -+-> Request2Env ---> UriParser --------->  BodyParser ----------> MethodOverrider -> Dispatcher ----------> [Action responds Env] ----------------------------------+
                                                                                                           | (both up |                      |                         |                                                                                                                                                                   |
                                                                                                  Response | and down |                      |                         |                                                                                                                      Response                                 Env |
  [downstream]                                          HttpResponseEncoder <----------------------------- | handler) | <--------------------+-------------------------+------------------------------------------------------------------------------------------------------------------------------ Env2Response <- ResponseCacher <----+

Xitrum handlers
---------------

See class xitrum.handler.ChannelPipelineFactory

Custom handler
--------------

TODO: improve Xitrum to let user customize the order of handlers
