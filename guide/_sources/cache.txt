Server-side cache
=================

Also see the chaper about :doc:`clustering </cluster>`.

Xitrum provides extensive client-side and server-side caching for faster responding.
At the web server layer, small files are cached in memory, big files are sent
using NIO's zero copy. Xitrum's static file serving speed is
`similar to that of Nginx <https://gist.github.com/3293596>`_.
At the web framework layer you have can declare page, action, and object cache
in the Rails style.
`All Google's best practices <http://code.google.com/speed/page-speed/docs/rules_intro.html>`_
like conditional GET are applied for client-side caching.

For dynamic content, if the content does not change after created (as if it is
a static file), you may set headers for clients to cache aggressively.
In that case, call ``setClientCacheAggressively()`` in your action.

Sometimes you may want to prevent client-side caching.
In that case, call ``setNoClientCache()`` in your action.

Server-side cache is discussed in more details below.

Cache page or action
--------------------

::

  import xitrum.Action
  import xitrum.annotation.{GET, CacheActionMinute, CachePageMinute}

  @GET("articles")
  @CachePageMinute(1)
  class ArticlesIndex extends Action {
    def execute() {
      ...
    }
  }

  @GET("articles/:id")
  @CacheActionMinute(1)
  class ArticlesShow extends Action {
    def execute() {
      ...
    }
  }

Cache object
------------

You use methods in ``xitrum.Config.xitrum.cache``, it's an instance of
`xitrum.Cache <http://ngocdaothanh.github.io/xitrum/api/index.html#xitrum.Cache>`_.

Without an explicit TTL (time to live):

* put(key, value)

Without an explicit TTL:

* putSecond(key, value, seconds)
* putMinute(key, value, minutes)
* putHour(key, value, hours)
* putDay(key, value, days)

Only if absent:

* putIfAbsent(key, value)
* putIfAbsentSecond(key, value, seconds)
* putIfAbsentMinute(key, value, minutes)
* putIfAbsentHour(key, value, hours)
* putIfAbsentDay(key, value, days)

Remove cache
------------

Remove page or action cache:

::

  removeAction[MyAction]

Remove object cache:

::

  remove(key)

Remove all keys that start with a prefix:

::

  removePrefix(keyPrefix)

With ``removePrefix``, you have the power to form hierarchical cache based on prefix.
For example you want to cache things related to an article, then when the article
changes, you want to remove all those things.

::

  import xitrum.Config.xitrum.cache

  // Cache with a prefix
  val prefix = "articles/" + article.id
  cache.put(prefix + "/likes", likes)
  cache.put(prefix + "/comments", comments)

  // Later, when something happens and you want to remove all cache related to the article
  cache.remove(prefix)

Config
------

The cache feature in Xitrum is provided by cache engines. You can choose the
engine that suits your need.

In `config/xitrum.conf <https://github.com/ngocdaothanh/xitrum-new/blob/master/config/xitrum.conf>`_,
you can config cache engine in one of the following 2 forms, depending on the engine you choose:

::

  cache = my.cache.EngineClassName

Or:

::

  cache {
    "my.cache.EngineClassName" {
      option1 = value1
      option2 = value2
    }
  }

Xitrum provides this one:

::

  cache {
    # Simple in-memory cache
    "xitrum.local.LruCache" {
      maxElems = 10000
    }
  }

If you have a cluster of servers, you can use `Hazelcast <https://github.com/ngocdaothanh/xitrum-hazelcast>`_.

If you want to create your own cache engine, implement the
`interface <https://github.com/ngocdaothanh/xitrum/blob/master/src/main/scala/xitrum/Cache.scala>`_
``xitrum.Cache``.

How cache works
---------------

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

xitrum.util.LocalLruCache
-------------------------

The above cache is the cache shared by the whole system. If you only want a
small convenient cache, you can use ``xitrum.util.LocalLruCache``.

::

  import xitrum.util.LocalLruCache

  // LRU (Least Recently Used) cache that can contain 1000 elements.
  // Keys and values are both of type String.
  val cache = LocalLruCache[String, String](1000)

The returned ``cache`` is a `java.util.LinkedHashMap <http://docs.oracle.com/javase/6/docs/api/java/util/LinkedHashMap.html>`_.
You can call ``LinkedHashMap`` methods from it.
