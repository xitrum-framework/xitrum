Dependencies
============

This chapter lists all dependency libraries that Xitrum uses so that in
your Xitrum project, you can use them directly if you want.

.. image:: deps.png

* `Scala <http://www.scala-lang.org/>`_:
  Xitrum is written in Scala language.
* `Netty <https://netty.io/>`_:
  For async HTTP(S) server. Many features in Xitrum are based on those in Netty,
  like WebSocket and zero copy file serving.
* `Akka <http://akka.io/>`_:
  For SockJS. Akka depends on `Typesafe Config <https://github.com/typesafehub/config>`_,
  which is also used by Xitrum.
* `Hazelcast <http://www.hazelcast.com/>`_:
  For distributing caches and server side sessions.
* `Rhino <https://developer.mozilla.org/en-US/docs/Rhino>`_:
  For Scalate to compile CoffeeScript to JavaScript.
* `JSON4S <https://github.com/json4s/json4s>`_:
  For parsing and generating JSON data. JSON4S depends on
  `Paranamer <http://paranamer.codehaus.org/>`_.
* `Sclasner <https://github.com/ngocdaothanh/sclasner>`_:
  For scanning HTTP routes in action classes in .class and .jar files.
* `Scaposer <https://github.com/ngocdaothanh/scaposer>`_:
  For i18n.
* `Commons Lang <http://commons.apache.org/lang/>`_:
  For escaping JSON data.
* `Twitter Chill <https://github.com/twitter/chill>`_:
  For serializing and deserializing cookies and sessions.
  Chill is based on `Kryo <http://code.google.com/p/kryo/>`_.
* `SLF4J <http://www.slf4j.org/>`_, `Logback <http://logback.qos.ch/>`_:
  For logging.

The default template engine in Xitrum is `xitrum-scalate <https://github.com/ngocdaothanh/xitrum-scalate>`_.
It depends on `Scalate <http://scalate.fusesource.org/>`_ and `Scalamd <https://github.com/chirino/scalamd>`_.
