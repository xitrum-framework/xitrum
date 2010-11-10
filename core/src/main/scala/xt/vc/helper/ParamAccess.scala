package xt.vc.helper

import xt.vc.Helper

import scala.collection.JavaConversions

class MissingParam(key: String) extends Throwable(key)

trait ParamAccess {
  this: Helper =>

  /**
   * Returns a singular element.
   */
  def param(key: String): String = {
    if (allParams.containsKey(key))
      allParams.get(key).get(0)
    else
      throw new MissingParam(key)
  }

  def paramo(key: String): Option[String] = {
    val values = allParams.get(key)
    if (values == null) None else Some(values.get(0))
  }

  /**
   * Returns a list of elements.
   */
  def params(key: String): List[String] = {
    if (allParams.containsKey(key))
      JavaConversions.asBuffer[String](allParams.get(key)).toList
    else
      throw new MissingParam(key)
  }

  def paramso(key: String): Option[List[String]] = {
    val values = allParams.get(key)
    if (values == null) None else Some(JavaConversions.asBuffer[String](values).toList)
  }
}
