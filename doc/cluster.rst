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
Please see ``hazelcast_mode`` in ``config/xitrum.properties``, ``config/hazelcast_cluster_member_or_super_client.xml``,
``config/hazelcast_java_client.properties``, and read `Hazelcast's documentation <http://www.hazelcast.com/documentation.jsp#Config>`_
to know how to config.

Session are stored in cookie by default. You don't need to worry how to share
sessions among Xitrum instances. But if you use :doc:`HazelcastSessionStore </scopes>`,
you may need to setup session replication by setting ``backup-count`` at the map
``xitrum/session`` in config/hazelcast_cluster_member_or_super_client.xml to more than 0.

xitrum.Config.hazelcastInstance
-------------------------------

Xitrum includes Hazelcast for cache and Comet. Thus, you can also use Hazelcast
in your Xitrum project yourself.

Hazelcast has `3 modes <http://www.hazelcast.com/documentation.jsp#Clients>`_:
cluster member, super client, and Java client. Please see ``hazelcast_mode``
in ``config/xitrum.properties``.

Xitrum handles these modes automatically. When you need to get a Hazelcast map,
do not do like this:

::

  import com.hazelcast.core.Hazelcast
  val myMap = Hazelcast.getMap("myMap")

You should do like this:

::

  import xitrum.Config.hazelcastInstance
  val myMap = Config.hazelcastInstance.getMap("myMap")
