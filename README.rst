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

To create a new xitrum project run in shell:
::
  git clone https://github.com/ngocdaothanh/xitrum-new my-app
  cd my-app
  sbt/sbt run

Now you have a new empty project running at http://localhost:8000/ and https://localhost:4430/

Support
---------------
* `Xitrum home page <http://ngocdaothanh.github.io/xitrum>`_
* `User guide <http://ngocdaothanh.github.io/xitrum/guide/index.html>`_
* `API doc <http://ngocdaothanh.github.io/xitrum/api/index.html>`_
* `Demos <https://github.com/ngocdaothanh/xitrum-demos>`_
* `Mailing list: "xitrum-framework" <https://groups.google.com/group/xitrum-framework>`_
