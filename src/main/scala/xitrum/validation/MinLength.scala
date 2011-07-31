package xitrum.validation

import xitrum.Action

object MinLength {
  def apply(length: Int) = new MinLength(length)
}

class MinLength(length: Int) extends Validator {
  def render(action: Action, paramName: String, secureParamName: String) {
    import action._
    jsAddToView(js$name(secureParamName) + ".rules('add', {minlength: " + length + "})")
  }

  def validate(action: Action, paramName: String, secureParamName: String): Boolean = {
    val value = action.param(paramName).trim
    value.length >= length
  }
}
