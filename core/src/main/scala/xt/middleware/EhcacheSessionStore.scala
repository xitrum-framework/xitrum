package xt.middleware

import xt._

import java.util.{HashMap => JMap}

import net.sf.ehcache.{CacheManager, Element}

class EhcacheSessionStore extends SessionStore {
  private val cache = CacheManager.getInstance.getCache(Config.sessionsEhcacheName)

  def read(id: String): Option[Session] = {
    val elem = cache.get(id)
    if (elem == null)
      None
    else {
      val data = elem.getObjectValue.asInstanceOf[JMap[String, Any]]
      val session = new Session(id, data, this)
      Some(session)
    }
  }

  def write(session: Session) {
    cache.put(new Element(session.id, session.data))
  }

  def delete(id: String) {
    cache.remove(id)
  }
}
