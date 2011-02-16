package xitrum.action.routing

object Util {
  /** Wraps a single String by a java.util.List. */
  def toValues(value: String): java.util.List[String] = {
    val values = new java.util.ArrayList[String](1)
    values.add(value)
    values
  }
}
