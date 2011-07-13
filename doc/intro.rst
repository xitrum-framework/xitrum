Introduction
============

.. image:: http://www.bdoubliees.com/journalspirou/sfigures6/schtroumpfs/s3.jpg

Xitrum ("Smurf" in Vietnamese) is a lightweight Scala web framework and web
server on top of `Netty <http://www.jboss.org/netty>`_:

* It fills the gap between `Scalatra <https://github.com/scalatra/scalatra>`_
  and `Lift <http://liftweb.net/>`_: more powerful than Scalatra and easier to
  use than Lift. You can easily create both RESTful APIs and postbacks. Xitrum
  is controller-first like Scalatra, not
  `view-first <http://www.assembla.com/wiki/show/liftweb/View_First>`_ like Lift.
* Typesafe, in the spirit of Scala.
* Asynchronous, in the spirit of Netty.
* Stateless, by default sessions are not stored on server.
* In-process and distribued cache using `Hazelcast <http://www.hazelcast.com/>`_,
  you don't need separate cache servers.
* Scalable `Comet <http://en.wikipedia.org/wiki/Comet_(programming)>`_,
  you can scale Comet to multiple servers.

::

  +-----------------+
  |    Your app     |
  +-----------------+
  |      Xitrum     |  <-- Hazelcast --> Other instances
  | +-------------+ |
  | | Action/View | |
  | +-------------+ |
  +-----------------+
  |      Netty      |
  | +-------------+ |
  | | HTTP Server | |
  | +-------------+ |
  +-----------------+

What do you create web applications for? There are 2 main use cases:

* To serve machines: you need to create RESTful APIs for smartphones, web services
  for other web sites.
* To serve human users: you need to create interactive web pages.

As a web framework, Xitrum aims to support you to solve these use cases easily.

Xitrum is inspired by `Nitrogen <http://nitrogenproject.com/>`_:

* Source code: https://github.com/ngocdaothanh/xitrum
* Documentation: http://ngocdaothanh.github.com/xitrum
* Google group: http://groups.google.com/group/xitrum-framework
