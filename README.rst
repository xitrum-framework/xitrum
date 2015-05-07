**Please see** `Xitrum home page <http://xitrum-framework.github.io/>`_ **for more details.**

.. image:: http://xitrum-framework.github.io/parts/whale.png

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

Xitrum is an open source project. See `list of contributors <https://github.com/xitrum-framework/xitrum/graphs/contributors>`_.
