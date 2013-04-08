package xitrum

class RequestVar[T] {
  private[this] val key = this.getClass.getName

  def get(implicit action: ActionEnv) = {
    action.at[T](key)
  }

  def set(value: T)(implicit action: ActionEnv) = {
    action.at(key) = value
    value
  }

  def isDefined(implicit action: ActionEnv) = {
    action.at.isDefinedAt(key)
  }
}
