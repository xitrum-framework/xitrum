Introduction
============

::

  +--------------------+
  |      Your app      |
  +--------------------+
  |    Xitrum fusion   |
  | +----------------+ |
  | | Web framework  | |  <-- Akka cluster --> Other instances
  | |----------------| |
  | | HTTP(S) Server | |
  | +----------------+ |
  +--------------------+
  |       Netty        |
  +--------------------+

Xitrum is an async and clustered Scala web framework and HTTP(S) server fusion
on top of `Netty <http://netty.io/>`_ and `Akka <http://akka.io/>`_.

From `a user <https://groups.google.com/group/xitrum-framework/msg/d6de4865a8576d39>`_:

  Wow, this is a really impressive body of work, arguably the most
  complete Scala framework outside of Lift (but much easier to use).

  `Xitrum <http://ngocdaothanh.github.com/xitrum>`_ is truly a full stack web framework, all the bases are covered,
  including wtf-am-I-on-the-moon extras like ETags, static file cache
  identifiers & auto-gzip compression. Tack on built-in JSON converter,
  before/around/after interceptors, request/session/cookie/flash scopes,
  integrated validation (server & client-side, nice), built-in cache
  layer (`Hazelcast <http://www.hazelcast.com/>`_), i18n a la GNU gettext, Netty (with Nginx, hello
  blazing fast), etc. and you have, wow.

Features
--------

* Typesafe, in the spirit of Scala. All the APIs try to be as typesafe as possible.
* Async, in the spirit of Netty. Your request proccessing action does not have
  to respond immediately. Long polling, chunked response (streaming), WebSocket,
  and SockJS are supported.
* Fast built-in HTTP and HTTPS web server based on `Netty <http://netty.io/>`_.
  Xitrum's static file serving speed is `similar to that of Nginx <https://gist.github.com/3293596>`_.
* Extensive client-side and server-side caching for faster responding.
  At the web server layer, small files are cached in memory, big files are sent
  using NIO's zero copy.
  At the web framework layer you have can declare page, action, and object cache
  in the Rails style.
  `All Google's best practices <http://code.google.com/speed/page-speed/docs/rules_intro.html>`_
  like conditional GET are applied for client-side caching.
  You can also force browsers to always send request to server to revalidate cache before using.
* Routes are automatically collected in the spirit of JAX-RS
  and Rails Engines. You don't have to declare all routes in a single place.
  Think of this feature as distributed routes. You can plug an app into another app.
  If you have a blog engine, you can package it as a JAR file, then you can put
  that JAR file into another app and that app automatically has blog feature!
  Routing is also two-way: you can recreate URLs (reverse routing) in a typesafe way.
  You can document routes using `Swagger Doc <http://swagger.wordnik.com/>`_.
* Views can be written in a separate `Scalate <http://scalate.fusesource.org/>`_
  template file or Scala inline XML. Both are typesafe.
* Sessions can be stored in cookies (more scalable) or clustered `Hazelcast <http://www.hazelcast.com/>`_ (more secure).
  Hazelcast also gives in-process (thus faster and simpler to use) distribued cache,
  you don't need separate cache servers. The same is for pubsub feature in Akka.
* `jQuery Validation <http://docs.jquery.com/Plugins/validation>`_ is integrated
  for browser side and server side validation.
* i18n using `GNU gettext <http://en.wikipedia.org/wiki/GNU_gettext>`_.
  Translation text extraction is done automatically.
  You don't have to manually mess with properties files.
  You can use powerful tools like `Poedit <http://www.poedit.net/screenshots.php>`_
  for translating and merging translations.
  gettext is unlike most other solutions, both singular and plural forms are supported.

Xitrum tries to fill the spectrum between `Scalatra <https://github.com/scalatra/scalatra>`_
and `Lift <http://liftweb.net/>`_: more powerful than Scalatra and easier to
use than Lift. You can easily create both RESTful APIs and postbacks. `Xitrum <http://ngocdaothanh.github.com/xitrum>`_
is controller-first like Scalatra, not
`view-first <http://www.assembla.com/wiki/show/liftweb/View_First>`_ like Lift.
Most people are familliar with controller-first style.

`Xitrum <http://ngocdaothanh.github.com/xitrum>`_ is `open source <https://github.com/ngocdaothanh/xitrum>`_, please join
its `Google group <http://groups.google.com/group/xitrum-framework>`_.

Samples
-------

* `Xitrum Demos <https://github.com/ngocdaothanh/xitrum-demos>`_
* `Xitrum Modularized Demo <https://github.com/ngocdaothanh/xitrum-modularized-demo>`_
* `Placeholder <https://github.com/georgeOsdDev/xitrum-placeholder>`_
* `Comy <https://github.com/ngocdaothanh/comy>`_
