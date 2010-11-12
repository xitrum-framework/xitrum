package xt.vc.env.session

import xt.vc.env.Session

trait SessionStore {
  def read(id: String): Option[Session]
  def write(session: Session)
  def delete(id: String)
}
