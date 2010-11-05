package xt.framework

import scala.collection.mutable.HashMap

/** Equivalent to @xxx variables of Rails */
class HelperAt {
  private val map = new HashMap[String, Any]()

  def apply[T](key: String): T        = map(key).asInstanceOf[T]
  def update(key: String, value: Any) { map.put(key, value) }
}
