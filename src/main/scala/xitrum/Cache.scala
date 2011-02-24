package xitrum

import java.util.concurrent.TimeUnit
import org.infinispan.{Cache => ICache}
import org.infinispan.manager.DefaultCacheManager

object Cache {
  val cache = {
    val manager = new DefaultCacheManager("infinispan.xml")
    manager.getCache("xitrum").asInstanceOf[ICache[String, Any]]
  }

  def tryCache(key: String, secs: Int)(f: => Any): Any = {
    val value = cache.get(key)
    if (value == null) {
      val value2 = f
      cache.put(key, value, secs, TimeUnit.SECONDS)
      value2
    } else {
      value
    }
  }
}
