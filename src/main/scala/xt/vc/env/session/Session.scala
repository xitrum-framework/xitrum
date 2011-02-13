package xt.vc.env.session

trait Session {
  def apply(key: String): Option[Any]
  def update(key: String, value: Any)
  def delete(key: String)
  def reset
}
