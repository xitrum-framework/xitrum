package xt.vc.session

import xt.Config

import net.sf.ehcache.{CacheManager, Element}

class EhcacheSessionStore extends SessionStore {
  private lazy val cache = {
    val name = Config.properties.getProperty("sessions_ehcache_name", "XitrumSessions")
    CacheManager.getInstance.getCache(name)
  }

  def read(id: String) = None
  def write(session: Session) {}
  def delete(id: String) {}
}
