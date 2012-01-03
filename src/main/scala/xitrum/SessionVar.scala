package xitrum

class SessionVar[T] {
  private val key = this.getClass.getName

  def get(implicit controller: Controller) = controller.session(key).asInstanceOf[T]

  def set(value: T)(implicit controller: Controller) = {
    controller.session(key) = value
    value
  }

  def isDefined(implicit controller: Controller) = controller.session.isDefinedAt(key)

  def remove(implicit controller: Controller) {
    controller.session.remove(key)
  }
}
