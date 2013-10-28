package xitrum.util

import java.util.{Collections, LinkedHashMap}
import java.util.Map.Entry

import xitrum.Cache

/**
 * Non-threadsafe, non-distributed LRU cache.
 *
 * http://stackoverflow.com/questions/221525/how-would-you-implement-an-lru-cache-in-java-6
 *
 * Xitrum uses this for storing etags for static files. Each web server in a
 * cluster has its own cache of (file path, mtime) -> etag.
 */
private class NonThreadsafeLocalLruCache[K, V](capacity: Int) extends LinkedHashMap[K, V](capacity + 1, 1.0f, true) {
  protected override def removeEldestEntry(eldest: Entry[K, V]) = size > capacity
}

object LocalLruCache {
  def apply[K, V](capacity: Int) = Collections.synchronizedMap(new NonThreadsafeLocalLruCache[K, V](capacity))
}

class LocalLruCache(maxElems: Int) extends Cache(maxElems) {
  private val cache = Collections.synchronizedMap(new NonThreadsafeLocalLruCache[Any, (Int, Any)](maxElems))

  def start() {}

  def stop() {}

  def isDefinedAt(key: Any) = cache.containsKey(key)

  def get(key: Any) = {
    val tuple = cache.get(key)
    if (tuple == null) {
      None
    } else {
      val expireSec = tuple._1
      val value     = tuple._2
      if (expireSec < 0) {
        Some(value)
      } else {
        val nowMs = System.currentTimeMillis()
        if (expireSec.toLong * 1000 < nowMs) None else Some(value)
      }
    }
  }

  def put(key: Any, value: Any) {
    cache.put(key, (-1, value))
  }

  def putIfAbsent(key: Any, value: Any): Unit = synchronized {
    if (!cache.containsKey(key)) cache.put(key, (-1, value))
  }

  def putSecond(key: Any, value: Any, seconds: Int) {
    cache.put(key, (seconds, value))
  }

  def putSecondIfAbsent(key: Any, value: Any, seconds: Int) {
    if (!cache.containsKey(key)) putSecond(key, value, seconds)
  }

  def remove(key: Any) {
    cache.remove(key)
  }

  def clear() {
    cache.clear()
  }
}
