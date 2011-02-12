package xt.vc.env.session

import xt.Config
import xt.vc.env.Session

// TODO
class CookieSessionStore extends SessionStore {
  def read(id: String) = {
    println("read: " + id)
    None
  }

  def write(session: Session) {
    println("write")
  }

  def delete(id: String) {
    println("delete: " + id)
  }
}
