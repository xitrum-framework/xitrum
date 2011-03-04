package xitrum

import java.io.File
import java.util.concurrent.TimeUnit
import com.hazelcast.core.Hazelcast
import com.hazelcast.core.IMap

object Cache extends Logger {
  val cache = {
    // http://code.google.com/p/hazelcast/wiki/Config
    // http://code.google.com/p/hazelcast/source/browse/trunk/hazelcast/src/main/java/com/hazelcast/config/XmlConfigBuilder.java
    val config = System.getProperty("user.dir") + File.separator + "config" + File.separator + "hazelcast.xml"
    System.setProperty("hazelcast.config", config)

    // http://code.google.com/p/hazelcast/issues/detail?id=94
    // http://code.google.com/p/hazelcast/source/browse/trunk/hazelcast/src/main/java/com/hazelcast/logging/Logger.java
    System.setProperty("hazelcast.logging.type", "slf4j")

    Hazelcast.getMap("xitrum").asInstanceOf[IMap[Any, Any]]
  }

  def putIfAbsentSecond(key: Any, value: Any, seconds: Int) = {
    logger.debug("putIfAbsent: " + key)
    cache.putIfAbsent(key, value, seconds, TimeUnit.SECONDS)
  }

  def putIfAbsentMinute(key: Any, value: Any, minutes: Int) = putIfAbsentSecond(key, value, minutes * 60)
  def putIfAbsentHour  (key: Any, value: Any, hours:   Int) = putIfAbsentMinute(key, value, hours   * 60)
  def putIfAbsentDay   (key: Any, value: Any, days:    Int) = putIfAbsentHour  (key, value, days    * 24)

  /**
   * Gets data from cache with type cast.
   * Application version up etc. may cause cache restoring to be failed.
   * In this case, we remove the cache.
   */
  def getAs[T](key: Any): Option[T] = {
    try {
      val value = cache.get(key)
      if (value != null) return Some(value.asInstanceOf[T]) else return None
    } catch {
      case e =>
        logger.warn("Cache data restoring failed, will now remove it, key: {}", key)
        cache.remove(key)
        None
    }
  }

  def tryCacheSecond[T](key: Any, secs: Int)(f: => T): T = {
    getAs[T](key) match {
      case Some(t) => t

      case None =>
        val value = f
        putIfAbsentSecond(key, value, secs)
        value
    }
  }

  def tryCacheMinute[T](key: String, minutes: Int)(f: => T): T = tryCacheSecond(key, minutes * 60)(f)
  def tryCacheHour[T]  (key: String, hours:   Int)(f: => T): T = tryCacheMinute(key, hours   * 60)(f)
  def tryCacheDay[T]   (key: String, days:    Int)(f: => T): T = tryCacheHour  (key, days    * 24)(f)
}
