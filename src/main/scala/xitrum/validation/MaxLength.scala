package xitrum.validation

import xitrum.action.Action

object MaxLength {
  def apply(length: Int) = new MaxLength(length)
}

class MaxLength(length: Int) extends Validator {
  def render(action: Action, paramName: String, securedParamName: String) {
    import action._

    val js = jsChain(
      jsByName(securedParamName),
      jsCall("rules", "\"add\"", "{maxlength: " + length + "}")
    )
    jsAddToView(js)
  }

  def validate(action: Action, paramName: String, securedParamName: String): Boolean = {
    val value = action.param(paramName).trim
    value.length <= length
  }
}
