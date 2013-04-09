package xitrum

class RequestVar[T] {
  private[this] val key = this.getClass.getName

  def get(implicit action: Action) = {
    action.at[T](key)
  }

  def set(value: T)(implicit action: Action) = {
    action.at(key) = value
    value
  }

  def isDefined(implicit action: Action) = {
    action.at.isDefinedAt(key)
  }
}
