package xt.vc.controller

import scala.collection.JavaConversions
import xt.{Controller, MissingParam}

trait ParamAccess {
  this: Controller =>

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
      JavaConversions.asScalaBuffer[String](allParams.get(key)).toList
    else
      throw new MissingParam(key)
  }

  def paramso(key: String): Option[List[String]] = {
    val values = allParams.get(key)
    if (values == null) None else Some(JavaConversions.asScalaBuffer[String](values).toList)
  }
}
