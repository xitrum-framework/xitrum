package xitrum.validation

import scala.xml.Elem
import xitrum.Action

object MinLength {
  def apply(length: Int) = new MinLength(length)
}

class MinLength(length: Int) extends Validator {
  def render(action: Action, elem: Elem, paramName: String, secureParamName: String): Elem = {
    import action._
    jsAddToView(js$name(secureParamName) + ".rules('add', {minlength: " + length + "})")
    elem
  }

  def validate(action: Action, paramName: String, secureParamName: String): Boolean = {
    val value = action.param(paramName).trim
    value.length >= length
  }
}
