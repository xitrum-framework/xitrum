package xt.vc.env.session

import xt.Config
import xt.vc.env.Session

// TODO
class EhcacheSessionStore extends SessionStore {
  def read(id: String) = None
  def write(session: Session) {}
  def delete(id: String) {}
}
