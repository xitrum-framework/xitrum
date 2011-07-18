Clustering with Hazelcast
=========================

.. image:: http://www.bdoubliees.com/journalspirou/sfigures6/schtroumpfs/s1.jpg

Xitrum is designed in mind to run in production environment as multiple instances
behind a proxy server or load balancer:

::

                                / Xitrum instance 1
  Load balancer/proxy server ---- Xitrum instance 2
                                \ Xitrum instance 3

Cache and Comet are clustered out of the box thanks to `Hazelcast <http://www.hazelcast.com/>`_.
Please see ``config/hazelcast.xml`` and read `Hazelcast's documentation <http://www.hazelcast.com/documentation.jsp#Config>`_
to know how to config.

Session are stored in cookie by default. You don't need to worry how to share
sessions among Xitrum instances.
