Cache
=====

.. image:: http://www.bdoubliees.com/journalspirou/sfigures6/schtroumpfs/s15.jpg

`Hazelcast <http://www.hazelcast.com/>`_
is integrated for page, action, and object cache. Of course you can
use it for other things (distributed processing etc.) in your application.

With Hazelcast, Xitrum instances become in-process memory cache servers. You don't
need seperate things like Memcache. Please see the chaper about :doc:`clustering </cluster>`.

::

                                / Xitrum/memory cache instance 1
  Load balancer/proxy server ---- Xitrum/memory cache instance 2
                                \ Xitrum/memory cache instance 3

Cache works with async response.

Page cache
----------

Action cache
------------

Object cache
------------

Cache expiration
----------------

* Expire page cache
* Expire action cache
* Expire object cache

Cache config
------------

Hazelcast is powerful. It supports distributed cache. Please see its documentation.

``config/hazelcast.xml`` sample:

::

  <?xml version="1.0" encoding="UTF-8"?>
  <hazelcast xsi:schemaLocation="http://www.hazelcast.com/schema/config hazelcast-basic.xsd"
             xmlns="http://www.hazelcast.com/schema/config"
             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
    <group>
      <name>myapp</name>
      <password>dev-pass</password>
    </group>

    <network>
      <port auto-increment="true">5701</port>
      <join>
        <multicast enabled="true">
          <multicast-group>224.2.2.3</multicast-group>
          <multicast-port>54327</multicast-port>
        </multicast>
        <tcp-ip enabled="true">
          <interface>127.0.0.1</interface>
        </tcp-ip>
      </join>
    </network>

    <!-- For page, action, object cache -->
    <map name="xitrum">
      <backup-count>0</backup-count>
      <eviction-policy>LRU</eviction-policy>
      <max-size>100000</max-size>
      <eviction-percentage>25</eviction-percentage>
    </map>
  </hazelcast>

Note that Xitrum instances of the same group (cluster) should have the same
``<group>/<name>``. Hazelcast provides a monitor tool, ``<group>/<password>``
is the password for the tool to connect to the group.

.. image:: http://www.hazelcast.com/resources/monitor-screen.png

Please see `Hazelcast's documentation <http://www.hazelcast.com/documentation.jsp#Monitoring>`_
for more information how to config ``config/hazelcast.xml``.

Internals
---------

Upstream

::

                 the action response
                 should be cached and
  request        the cache already exists?
  -------------------------+---------------NO--------------->
                           |
  <---------YES------------+
    respond from cache


Downstream

::

                 the action response
                 should be cached and
                 the cache does not exist?           response
  <---------NO-------------+---------------------------------
                           |
  <---------YES------------+
    store response to cache
