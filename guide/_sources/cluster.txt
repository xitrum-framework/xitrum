Clustering with Akka
====================

Xitrum is designed in mind to run in production environment as multiple instances
behind a proxy server or load balancer:

::

                                / Xitrum instance 1
  Load balancer/proxy server ---- Xitrum instance 2
                                \ Xitrum instance 3

Cache, sessions, and SockJS sessions can be clustered out of the box thanks to
`Akka <http://akka.io/>`_ and `Hazelcast <http://www.hazelcast.com/>`_.

For sessions, of course you can store them in cookie. If you do so, you don't need
to store them in :doc:`HazelcastSessionStore cluster </scopes>`.

Please see ``config/akka.conf`` and and read `Akka doc <http://akka.io/docs/>`_
to know how to config.
