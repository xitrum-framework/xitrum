package xitrum.scope

import scala.collection.mutable.{Map => MMap}

/** Equivalent to @xxx variables of Rails */
class At {
  private val map = MMap[String, Any]()

  def apply[T](key: String) = map(key).asInstanceOf[T]

  def update(key: String, value: Any) { map.put(key, value) }

  def isDefinedAt(key: String) = map.isDefinedAt(key)
}
