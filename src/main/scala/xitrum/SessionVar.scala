package xitrum

class SessionVar[T] {
  private[this] val key = this.getClass.getName

  def get(implicit action: ActionEnv) = action.session(key).asInstanceOf[T]

  def set(value: T)(implicit action: ActionEnv) = {
    action.session(key) = value
    value
  }

  def isDefined(implicit action: ActionEnv) = action.session.isDefinedAt(key)

  def remove()(implicit action: ActionEnv) {
    action.session.remove(key)
  }
}
