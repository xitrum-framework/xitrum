Async and clustered Scala web framework and HTTP(S) server for Rails developers
===============================================================================

::

  +--------------------+
  |      Your app      |
  +--------------------+
  |    Xitrum fusion   |
  | +----------------+ |
  | | Web framework  | |  <-- Akka, Hazelcast --> Other instances
  | |----------------| |
  | | HTTP(S) Server | |
  | +----------------+ |
  +--------------------+
  |       Netty        |
  +--------------------+

Xitrum is an async and clustered Scala web framework and HTTP(S) server fusion
on top of `Netty <http://netty.io/>`_, `Akka <http://akka.io/>`_, and
`Hazelcast <http://www.hazelcast.com/>`_.

Please see `Xitrum home page <http://ngocdaothanh.github.com/xitrum>`_.
