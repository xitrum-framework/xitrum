package xitrum.local

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

  def start(): Unit = {}
  def stop(): Unit = {}

  def get(sessionId: String): Option[Map[String, Any]] = Option(store.get(sessionId))

  def put(sessionId: String, immutableMap: Map[String, Any]): Unit = {
    store.put(sessionId, immutableMap)
  }

  def remove(sessionId: String): Unit = {
    store.remove(sessionId)
  }
}
