Please see `Xitrum home page <http://xitrum-framework.github.io/xitrum/>`_.

.. image:: http://xitrum-framework.github.io/xitrum/parts/whale.png

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
  |       Xitrum       |
  | +----------------+ |
  | | HTTP(S) Server | |
  | |----------------| |
  | | Web framework  | |  ← Akka, Hazelcast → Other instances
  | +----------------+ |
  +--------------------+
  |      Your app      |
  +--------------------+
