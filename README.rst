Please see `Xitrum home page <http://ngocdaothanh.github.io/xitrum>`_.

.. image:: http://ngocdaothanh.github.io/xitrum/parts/whale.png

Xitrum is an async and clustered Scala web framework and HTTP(S) server fusion
on top of `Netty <http://netty.io/>`_, `Akka <http://akka.io/>`_, and
`Hazelcast <http://www.hazelcast.com/>`_.

::

  +--------------------+
  |      Your app      |
  +--------------------+
  |        Xitrum      |
  | +----------------+ |
  | | Web framework  | |  <-- Akka, Hazelcast --> Other instances
  | |----------------| |
  | | HTTP(S) Server | |
  | +----------------+ |
  +--------------------+
  |       Netty        |
  +--------------------+


Getting started
---------------

Create a new Xitrum project in 3 simple steps:

::

  git clone https://github.com/ngocdaothanh/xitrum-new my-app
  cd my-app
  sbt/sbt run

Now you have a new empty project running at http://localhost:8000/ and https://localhost:4430/

To generate Eclipse or IntelliJ project:

::

  sbt/sbt eclipse
  sbt/sbt gen-idea
