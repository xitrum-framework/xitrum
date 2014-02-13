package xitrum.local

import scala.collection.mutable.{Map => MMap}

import xitrum.Config
import xitrum.scope.session.ServerSessionStore
import xitrum.util.LocalLruCache

/**
 * Config in xitrum.conf:
 *
 * {{{
 * xitrum {
 *   session {
 *     store {
 *       "xitrum.local.LruSessionStore" {
 *         maxElems = 10000
 *       }
 *     }
 *   }
 * }
 * }}}
 */
class LruSessionStore extends ServerSessionStore {
  private[this] val store = {
    val className = getClass.getName
    val maxElems  = Config.xitrum.config.getInt("session.store.\"" + className + "\".maxElems")
    LocalLruCache[String, Map[String, Any]](maxElems)
  }

  def start() {}
  def stop() {}

  def get(sessionId: String) = Option(store.get(sessionId))

  def put(sessionId: String, immutableMap: Map[String, Any]) {
    store.put(sessionId, immutableMap)
  }

  def remove(sessionId: String) {
    store.remove(sessionId)
  }
}
