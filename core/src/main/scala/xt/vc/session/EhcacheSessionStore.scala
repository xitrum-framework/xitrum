package xt.vc.session

class EhcacheSessionStore extends SessionStore {
  def read(id: String) = None
  def write(session: Session) {}
  def delete(id: String) {}
}
