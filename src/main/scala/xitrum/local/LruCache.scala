package xitrum.local

import java.util.Collections
import com.typesafe.config.ConfigObject

import xitrum.{Cache, Config}
import xitrum.util.LocalLruCache

class LruCache extends Cache {
  private[this] val cache = {
    // xitrum {
    //   cache {
    //     "xitrum.local.LruCache" {
    //       maxElems = 10000
    //     }
    //   }
    // }
    val className = getClass.getName
    val maxElems  = Config.xitrum.config.getInt("cache.\"" + className + "\".maxElems")

    // Use Int for expireAtSec, not Long, because we only need second precision,
    // not millisenod precision:
    //            key   expireAtSec value
    LocalLruCache[Any, (Int,        Any)](maxElems)
  }

  def start() {}
  def stop() {}

  def isDefinedAt(key: Any) = cache.containsKey(key)

  def get(key: Any) = {
    val tuple = cache.get(key)
    if (tuple == null) {
      None
    } else {
      val expireAtSec = tuple._1
      val value       = tuple._2
      if (expireAtSec < 0) {
        Some(value)
      } else {
        // Compare at millisec precision for a little more correctness, so that
        // when TTL is 1s and 1500ms has passed, the result will more likely be
        // None
        val nowMs = System.currentTimeMillis()
        if (expireAtSec.toLong * 1000L < nowMs) None else Some(value)
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
    val expireAtSec = (System.currentTimeMillis() / 1000L + seconds).toInt
    cache.put(key, (expireAtSec, value))
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
