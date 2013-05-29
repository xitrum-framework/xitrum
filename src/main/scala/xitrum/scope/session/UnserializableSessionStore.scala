package xitrum.scope.session

import scala.collection.mutable.{Map => MMap}

class UnserializableSessionStore extends ServerSessionStore {
  private[this] val store = MMap[String, Map[String, Any]]()

  def get(sessionId: String): Option[Map[String, Any]] =
    store.get(sessionId)

  def put(sessionId: String, immutableMap: Map[String, Any]) {
    store.put(sessionId, immutableMap)
  }

  def remove(sessionId: String) {
    store.remove(sessionId)
  }
}
