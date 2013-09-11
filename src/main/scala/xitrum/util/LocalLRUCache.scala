package xitrum.util

import java.util.{Collections, LinkedHashMap}
import java.util.Map.Entry

/**
 * Non-threadsafe, non-distributed LRU cache.
 *
 * See:
 * http://littletechsecrets.wordpress.com/2008/11/16/simple-lru-cache-in-java/
 * http://amix.dk/blog/post/19465
 *
 * Xitrum uses this for storing etags for static files. Each web server in a
 * cluster has its own cache of (path, mtime) -> etag.
 */
private class NonThreadsafeLocalLRUCache[K, V](capacity: Int) extends LinkedHashMap[K, V](capacity + 1, 1.0f, true) {
  protected override def removeEldestEntry(eldest: Entry[K, V]) = size > capacity
}

object LocalLRUCache {
  def apply[K, V](capacity: Int) = Collections.synchronizedMap(new NonThreadsafeLocalLRUCache[K, V](capacity))
}
