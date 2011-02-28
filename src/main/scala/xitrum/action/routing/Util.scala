package xitrum.action.routing

object Util {
  /** Wraps a single String by a List. */
  def toValues[T](value: T): List[T] = List(value)
}
