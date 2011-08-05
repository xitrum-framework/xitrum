package xitrum.validation

import xitrum.Action

trait Validator extends Serializable {
  def render(action: Action, paramName: String, secureParamName: String)
  def validate(action: Action, paramName: String, secureParamName: String): Boolean
}
