package xt.vc.env

import scala.collection.mutable.HashMap

/** Equivalent to @xxx variables of Rails */
class At {
  private val map = new HashMap[String, Any]()

  def apply[T](key: String) = map(key).asInstanceOf[T]

  def update(key: String, value: Any) { map.put(key, value) }
}
