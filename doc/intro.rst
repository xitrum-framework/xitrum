Introduction
============

.. image:: http://www.bdoubliees.com/journalspirou/sfigures6/schtroumpfs/s3.jpg

Xitrum ("Smurf" in Vietnamese) is an async and clustered Scala web framework and
web server on top of `Netty <http://www.jboss.org/netty>`_ and
`Hazelcast <http://www.hazelcast.com/>`_:

* It fills the gap between `Scalatra <https://github.com/scalatra/scalatra>`_
  and `Lift <http://liftweb.net/>`_: more powerful than Scalatra and easier to
  use than Lift. You can easily create both RESTful APIs and postbacks. Xitrum
  is controller-first like Scalatra, not
  `view-first <http://www.assembla.com/wiki/show/liftweb/View_First>`_ like Lift.
* Annotation is used for URL routes, in the spirit of JAX-RS. You don't have to
  declare all routes in a single place.
* Typesafe, in the spirit of Scala.
* Async, in the spirit of Netty.
* Sessions can be stored in cookies or clustered Hazelcast.
* `jQuery Validation <http://docs.jquery.com/Plugins/validation>`_ is integrated
  for browser side and server side validation.
* i18n using `GNU gettext <http://en.wikipedia.org/wiki/GNU_gettext>`_.

Hazelcast also gives:

* In-process and distribued cache, you don't need separate cache servers.
* In-process and clustered `Comet <http://en.wikipedia.org/wiki/Comet_(programming)>`_,
  you don't need a separate Comet server.

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

Xitrum is open source:

* Source code: https://github.com/ngocdaothanh/xitrum
* Google group: http://groups.google.com/group/xitrum-framework
