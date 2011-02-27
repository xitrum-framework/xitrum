package xitrum.action.env

import scala.collection.JavaConversions

import xitrum.action.Action
import xitrum.action.exception.MissingParam

trait ParamAccess {
  this: Action =>

  /**
   * Returns a singular element.
   */
  def param(key: String, coll: Env.Params = null): String = {
    val coll2 = if (coll == null) textParams else coll
    if (coll2.containsKey(key)) coll2.get(key).get(0) else throw new MissingParam(key)
  }

  def paramo(key: String, coll: Env.Params = null): Option[String] = {
    val coll2 = if (coll == null) textParams else coll
    val values = coll2.get(key)
    if (values == null) None else Some(values.get(0))
  }

  /**
   * Returns a list of elements.
   */
  def params(key: String, coll: Env.Params = null): List[String] = {
    val coll2 = if (coll == null) textParams else coll
    if (coll2.containsKey(key))
      JavaConversions.asScalaBuffer[String](coll2.get(key)).toList
    else
      throw new MissingParam(key)
  }

  def paramso(key: String, coll: Env.Params = null): Option[List[String]] = {
    val coll2 = if (coll == null) textParams else coll
    val values = coll2.get(key)
    if (values == null) None else Some(JavaConversions.asScalaBuffer[String](values).toList)
  }

  //----------------------------------------------------------------------------

  def tparam[T](key: String, coll: Env.Params = null)(implicit m: Manifest[T]): T = {
    val value = param(key, coll)
    convert[T](value)
  }

  def tparamo[T](key: String, coll: Env.Params = null)(implicit m: Manifest[T]): Option[T] = {
    val valueo = paramo(key, coll)
    valueo.map(convert[T](_))
  }

  def tparams[T](key: String, coll: Env.Params = null)(implicit m: Manifest[T]): List[T] = {
    val values = params(key, coll)
    values.map(convert[T](_))
  }

  def tparamso[T](key: String, coll: Env.Params = null)(implicit m: Manifest[T]): Option[List[T]] = {
    paramso(key, coll) match {
      case None         => None
      case Some(values) => Some(values.map(convert[T](_)))
    }
  }

  private def convert[T](value: String)(implicit m: Manifest[T]): T = {
    val v = m.toString match {
      case "int"    => value.toInt
      case "short"  => value.toShort
      case "long"   => value.toLong
      case "float"  => value.toFloat
      case "double" => value.toDouble
      case unknown  => throw new Exception("Cannot covert String to " + unknown)
    }
    v.asInstanceOf[T]
  }
}
