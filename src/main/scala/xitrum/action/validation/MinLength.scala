package xitrum.action.validation

import xitrum.action.Action

class MinLength(length: Int) extends Validator {
  def render(action: Action, paramName: String, securedParamName: String) {
    import action._

    val js = jsChain(
      jsByName(securedParamName),
      jsCall("rules", "\"add\"", "{minlength: " + length + "}")
    )
    jsAddToView(js)
  }

  def validate(action: Action, paramName: String, securedParamName: String): Boolean = {
    val value = action.param(paramName).trim
    value.length >= length
  }
}
