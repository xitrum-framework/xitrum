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
    LocalLruCache[Any, (Int, Any)](maxElems)
  }

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
