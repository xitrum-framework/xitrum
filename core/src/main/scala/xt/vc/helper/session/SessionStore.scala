package xt.vc.helper.session

trait SessionStore {
  def read(id: String): Option[Session]
  def write(session: Session)
  def delete(id: String)
}
