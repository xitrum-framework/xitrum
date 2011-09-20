package xitrum.util

import java.util.LinkedHashMap
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
class LocalLRUCache[K, V](capacity: Int) extends LinkedHashMap[K, V](capacity + 1, 0.75f, true) {
  protected override def removeEldestEntry(eldest: Entry[K, V]) = size > capacity
}
