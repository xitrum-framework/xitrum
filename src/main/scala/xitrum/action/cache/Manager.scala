package xitrum.action.cache

import org.infinispan.{Cache => ICache}
import org.infinispan.manager.DefaultCacheManager

object Manager {
  val cache = {
    val manager = new DefaultCacheManager("infinispan.xml")
    manager.getCache("xitrum").asInstanceOf[ICache[String, Any]]
  }
}
