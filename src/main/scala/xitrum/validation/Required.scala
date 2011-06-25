package xitrum.validation

import xitrum.Action

object Required extends Validator {
  def render(action: Action, paramName: String, secureParamName: String) {
    import action._

    val js = jsChain(
      jsByName(secureParamName),
      jsCall("rules", "\"add\"", "{required: true}")
    )
    jsAddToView(js)
  }

  def validate(action: Action, paramName: String, secureParamName: String): Boolean = {
    val value = action.param(paramName).trim
    !value.isEmpty
  }
}
