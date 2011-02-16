package xitrum.action.validation

import xitrum.action.Action

class Required extends Validator {
  def render(action: Action, paramName: String, securedParamName: String) {
    import action._

    val js = jsChain(
      jsByName(securedParamName),
      jsCall("rules", "\"add\"", "{required: true}")
    )
    jsAddToView(js)
  }

  def validate(action: Action, paramName: String, securedParamName: String): Boolean = {
    true
  }
}
