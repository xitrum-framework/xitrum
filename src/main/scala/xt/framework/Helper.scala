package xt.framework

import scala.collection.mutable.{Map, ListBuffer}
import scala.collection.JavaConversions

trait Helper {
  private var _params: java.util.Map[String, java.util.List[String]] = _

  def setParams(params: java.util.Map[String, java.util.List[String]]) { _params = params }

  /**
   * Returns a singular element.
   */
  def param(key: String): Option[String] = {
    val values = _params.get(key)
    if (values == null) None else Some(values.get(0))
  }

  /**
   * Returns a list of elements.
   */
  def params(key: String): Option[List[String]] = {
    val values = _params.get(key)
    if (values == null) None else Some(JavaConversions.asBuffer[String](values).toList)
  }

  //----------------------------------------------------------------------------

  // Equivalent to @xxx variables of Rails
  private var _at: Map[String, Any] = _

  def setAt(at: Map[String, Any]) { _at = at }

  def at(key: String, value: Any) = _at.put(key, value)
  def at[T](key: String): T       = _at(key).asInstanceOf[T]
}
