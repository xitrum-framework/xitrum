package xitrum.validation

import xitrum.Action

object MaxLength {
  def apply(length: Int) = new MaxLength(length)
}

class MaxLength(length: Int) extends Validator {
  def render(action: Action, paramName: String, secureParamName: String) {
    import action._
    jsAddToView(js$name(secureParamName) + ".rules('add', {maxlength: " + length + "})")
  }

  def validate(action: Action, paramName: String, secureParamName: String): Boolean = {
    val value = action.param(paramName).trim
    value.length <= length
  }
}
