package xitrum

class SessionVar[T] {
  def get(implicit action: Action) = {
    val key = this.getClass.getName
    action.session[T](key)
  }

  def set(value: T)(implicit action: Action) = {
    val key = this.getClass.getName
    action.session(key) = value
    value
  }

  def isDefined(implicit action: Action) = {
    val key = this.getClass.getName
    action.session.contains(key)
  }

  def delete(implicit action: Action) {
    val key = this.getClass.getName
    action.session.delete(key)
  }
}
