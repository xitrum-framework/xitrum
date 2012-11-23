package xitrum.scope.request

import scala.collection.mutable.HashMap

/** Equivalent to @xxx variables of Rails */
class At extends HashMap[String, Any] {
  def apply[T](key: String): T = super.apply(key).asInstanceOf[T]
}
