package xitrum

class RequestVar[T] {
  private val key = this.getClass.getName

  def get(implicit controller: Controller) = {
    controller.at[T](key)
  }

  def set(value: T)(implicit controller: Controller) = {
    controller.at(key) = value
    value
  }

  def isDefined(implicit controller: Controller) = {
    controller.at.isDefinedAt(key)
  }
}
