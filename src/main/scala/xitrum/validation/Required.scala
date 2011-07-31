package xitrum.validation

import xitrum.Action

object Required extends Validator {
  def render(action: Action, paramName: String, secureParamName: String) {
    import action._
    jsAddToView(js$name(secureParamName) + ".rules('add', {required: true})")
  }

  def validate(action: Action, paramName: String, secureParamName: String): Boolean = {
    val value = action.param(paramName).trim
    !value.isEmpty
  }
}
