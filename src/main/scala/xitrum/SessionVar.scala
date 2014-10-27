package xitrum

class SessionVar[T] {
  private[this] val key = this.getClass.getName

  def get(implicit action: Action) = action.session(key).asInstanceOf[T]

  def set(value: T)(implicit action: Action) = {
    action.session(key) = value
    value
  }

  def isDefined(implicit action: Action) = action.session.isDefinedAt(key)

  def isEmpty(implicit action: Action) = !action.session.isDefinedAt(key)

  def remove()(implicit action: Action) {
    action.session.remove(key)
  }
}
