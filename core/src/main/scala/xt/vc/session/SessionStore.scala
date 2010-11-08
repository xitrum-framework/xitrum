package xt.vc.session

trait SessionStore {
  def read(id: String): Option[Session]
  def write(session: Session)
  def delete(id: String)
}
