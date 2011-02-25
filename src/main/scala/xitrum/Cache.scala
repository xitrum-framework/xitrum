package xitrum

import java.io.File
import java.util.concurrent.TimeUnit
import com.hazelcast.core.Hazelcast
import com.hazelcast.core.IMap

object Cache {
  val cache = {
    // http://code.google.com/p/hazelcast/wiki/Config
    val config = System.getProperty("user.dir") + File.separator + "config" + File.separator + "hazelcast.xml"
    System.setProperty("hazelcast.config", config)

    Hazelcast.getMap("xitrum").asInstanceOf[IMap[String, Any]]
  }

  def tryCache(key: String, secs: Int)(f: => Any): Any = {
    val value = cache.get(key)
    if (value != null) return value

    val value2 = f
    cache.putIfAbsent(key, value2, secs, TimeUnit.SECONDS)
    value2
  }
}
