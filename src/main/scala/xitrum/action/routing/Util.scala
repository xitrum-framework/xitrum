package xitrum.action.routing

object Util {
  /** Wraps a single String by a java.util.List. */
  def toValues[T](value: T): java.util.List[T] = {
    val values = new java.util.ArrayList[T](1)
    values.add(value)
    values
  }
}
