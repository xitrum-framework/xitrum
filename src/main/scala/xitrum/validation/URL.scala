package xitrum.validation

import xitrum.Action

object URL extends Validator {
  def render(action: Action, paramName: String, secureParamName: String) {
    import action._
    jsAddToView(js$name(secureParamName) + ".rules('add', {url: true})")
  }

  def validate(action: Action, paramName: String, secureParamName: String): Boolean = {
    val value = action.param(paramName).trim
    value.contains("://")
  }
}
