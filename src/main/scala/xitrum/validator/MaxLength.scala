package xitrum.validator

import scala.xml.Elem
import xitrum.Controller

object MaxLength {
  def apply(length: Int) = new MaxLength(length)
}

class MaxLength(length: Int) extends Validator {
  def render(controller: Controller, elem: Elem, paramName: String, secureParamName: String): Elem = {
    import controller._
    jsAddToView(js$name(secureParamName) + ".rules('add', {maxlength: " + length + "})")
    elem
  }

  def validate(controller: Controller, paramName: String, secureParamName: String): Boolean = {
    val value = controller.param(paramName).trim
    value.length <= length
  }
}
