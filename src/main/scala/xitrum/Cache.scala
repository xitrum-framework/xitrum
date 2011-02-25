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

    Hazelcast.getMap("xitrum").asInstanceOf[IMap[String, Any]]
  }

  def tryCacheSecond[T](key: Any, secs: Int)(f: => T): T = {
    val key2  = key.toString
    val value = cache.get(key2)
    if (value != null) return value.asInstanceOf[T]

    val value2 = f

    logger.debug("putIfAbsent: " + key2)
    cache.putIfAbsent(key2, value2, secs, TimeUnit.SECONDS)

    value2
  }

  def tryCacheDay[T]   (key: String, days:    Int)(f: => T): T = tryCacheSecond(key, days * 24 * 60 * 60)(f)
  def tryCacheHour[T]  (key: String, hours:   Int)(f: => T): T = tryCacheSecond(key, hours     * 60 * 60)(f)
  def tryCacheMinute[T](key: String, minutes: Int)(f: => T): T = tryCacheSecond(key, minutes        * 60)(f)
}
