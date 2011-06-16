Cache
=====

.. image:: http://www.bdoubliees.com/journalspirou/sfigures6/schtroumpfs/s15.jpg

`Hazelcast <http://www.hazelcast.com/>`_
is integrated for page, action, and object cache. Of course you can
use it for other things (distributed processing etc) in your application.

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

Internal
--------

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
