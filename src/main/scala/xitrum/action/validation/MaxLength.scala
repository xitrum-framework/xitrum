package xitrum.action.validation

import xitrum.action.Action

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
    true
  }
}
