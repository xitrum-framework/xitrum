package xitrum.validator

import scala.xml.Elem
import xitrum.Controller

trait Validator extends Serializable {
  def render(controller: Controller, elem: Elem, paramName: String, secureParamName: String): Elem
  def validate(controller: Controller, paramName: String, secureParamName: String): Boolean

  //----------------------------------------------------------------------------

  def ::(other: Validator) = new Validators(List(other, this))

  def ::(elem: Elem)(implicit controller: Controller): Elem = {
    val validators = new Validators(List(this))
    elem :: validators
  }
}
