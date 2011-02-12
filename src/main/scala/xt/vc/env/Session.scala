package xt.vc.env

import java.util.{UUID, HashMap => JMap, Set => JSet}
import xt.vc.env.session.SessionStore

class Session(var id: String, var data: JMap[String, Any], store: SessionStore) {
  def reset {
    store.delete(id)
    data.clear
    id = UUID.randomUUID.toString
    update("_csrf_token", UUID.randomUUID.toString)
  }

  def apply(key: String) = data.get(key)

  def update(key: String, value: Any) {
    data.put(key, value)
  }
}
