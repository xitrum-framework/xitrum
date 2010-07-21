package xt.framework

import scala.collection.mutable.HashMap

trait Env {
  var _params: HashMap[String, Any] = _
  var _at:     HashMap[String, Any] = _

  def params[T](key: String): T   = _params(key).asInstanceOf[T]
  def at(key: String, value: Any) = _at.put(key, value)
  def at[T](key: String): T       = _at(key).asInstanceOf[T]
}
