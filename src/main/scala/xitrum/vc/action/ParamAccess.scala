package xitrum.vc.action

import scala.collection.JavaConversions

import xitrum.vc.env.Env
import xitrum.{Action, MissingParam}

trait ParamAccess {
  this: Action =>

  /**
   * Returns a singular element.
   */
  def param(key: String, coll: Env.Params = null): String = {
    val coll2 = if (coll == null) allParams else coll
    if (coll2.containsKey(key)) coll2.get(key).get(0) else throw new MissingParam(key)
  }

  def paramo(key: String, coll: Env.Params = null): Option[String] = {
    val coll2 = if (coll == null) allParams else coll
    val values = coll2.get(key)
    if (values == null) None else Some(values.get(0))
  }

  /**
   * Returns a list of elements.
   */
  def params(key: String, coll: Env.Params = null): List[String] = {
    val coll2 = if (coll == null) allParams else coll
    if (coll2.containsKey(key))
      JavaConversions.asScalaBuffer[String](coll2.get(key)).toList
    else
      throw new MissingParam(key)
  }

  def paramso(key: String, coll: Env.Params = null): Option[List[String]] = {
    val coll2 = if (coll == null) allParams else coll
    val values = coll2.get(key)
    if (values == null) None else Some(JavaConversions.asScalaBuffer[String](values).toList)
  }
}
