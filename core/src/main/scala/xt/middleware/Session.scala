package xt.middleware

import xt._

import java.util.{UUID, HashMap => JMap, Set => JSet}

import org.jboss.netty.channel.Channel
import org.jboss.netty.handler.codec.http.{HttpRequest, HttpResponse, Cookie => NCookie, DefaultCookie}

class Session(var id: String, var data: JMap[String, Any], store: SessionStore) {
  def reset {
    store.delete(id)
    data.clear
    id = UUID.randomUUID.toString
  }

  def apply(key: String) = data.get(key)

  def update(key: String, value: Any) {
    data.put(key, value)
  }
}

trait SessionStore {
  def read(id: String): Option[Session]
  def write(session: Session)
  def delete(id: String)
}

object Session {
  def wrap(app: App, store: SessionStore) = new App {
    def call(channel: Channel, request: HttpRequest, response: HttpResponse, env: Env) {
      val id = restoreSessionId(env, store)
      env.session = store.read(id) match {
        case Some(session) => session
        case None          => new Session(id, new JMap[String, Any](), store)
      }

      app.call(channel, request, response, env)

      store.write(env.session)
      storeSessionId(env)
    }
  }

  //----------------------------------------------------------------------------

  private def restoreSessionId(env: Env, store: SessionStore): String = {
    findSessionCookie(env.cookies) match {
      case Some(cookie) =>
        cookie.getValue

      case None =>
        // No cookie, try from the request parameters
        val xs = env.params.get(Config.sessionIdName)
        if (xs == null)
          UUID.randomUUID.toString
        else {
           val sid = xs.get(0)
           checkSessionId(sid, store)
        }
    }
  }

  private def storeSessionId(env: Env) {
    findSessionCookie(env.cookies) match {
      case Some(cookie) => cookie.setValue(env.session.id)

      case None =>
        val cookie = new DefaultCookie(Config.sessionIdName, env.session.id)
        cookie.setPath("/")
        env.cookies.add(cookie)
    }
  }

  /** Take out the cookie that stores the session ID */
  private def findSessionCookie(cookies: JSet[NCookie]): Option[NCookie] = {
    val iter = cookies.iterator
    while (iter.hasNext) {
      val cookie = iter.next
      if (cookie.getName == Config.sessionIdName) return Some(cookie)
    }
    None
  }

  /**
   * Session ID from request parameters is prone to session fixation
   * => Avoid this problem by looking up the session store and create
   * a new ID if the store does not contain the ID
   */
  private def checkSessionId(id: String, store: SessionStore): String = {
    store.read(id) match {
      case None => UUID.randomUUID.toString
      case _    => id
    }
  }
}
