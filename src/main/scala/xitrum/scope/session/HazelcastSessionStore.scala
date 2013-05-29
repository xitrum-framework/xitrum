package xitrum.scope.session

import com.hazelcast.core.IMap
import xitrum.Config

class HazelcastSessionStore extends ServerSessionStore {
  // We can use Cache, but we use a separate Hazelcast map to avoid the cost of
  // iterating through a big map as much as we can. Another reason is that the
  // application may need to config Hazelcast to persist sessions to a place
  // (disk, DB etc.) different to those for other things (cache, comet etc.).
  private[this] val store = Config.hazelcastInstance.getMap("xitrum/session").asInstanceOf[IMap[String, Map[String, Any]]]

  def get(sessionId: String): Option[Map[String, Any]] = Option(store.get(sessionId))

  def put(sessionId: String, immutableMap: Map[String, Any]) {
    store.put(sessionId, immutableMap)
  }

  def remove(sessionId: String) {
    store.remove(sessionId)
  }
}
