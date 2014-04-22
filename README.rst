Please see `Xitrum home page <http://ngocdaothanh.github.io/xitrum>`_.

.. image:: http://ngocdaothanh.github.io/xitrum/parts/whale.png

Xitrum is an async and clustered Scala web framework on top of
`Netty <http://netty.io/>`_, `Akka <http://akka.io/>`_, and
`Hazelcast <http://www.hazelcast.com/>`_.
It also has a fast built-in HTTP(S) server.

::

  +--------------------+
  |      Clients       |
  +--------------------+
          ↓   ↑
  +--------------------+
  |       Netty        |
  +--------------------+
  |        Xitrum      |
  | +----------------+ |
  | | Web framework  | |  <-- Akka, Hazelcast --> Other instances
  | |----------------| |
  | | HTTP(S) Server | |
  | +----------------+ |
  +--------------------+
  |      Your app      |
  +--------------------+
