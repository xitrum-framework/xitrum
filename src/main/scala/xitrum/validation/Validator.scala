package xitrum.validation

import java.io.Serializable
import xitrum.action.Action

trait Validator extends Serializable {
  def render(action: Action, paramName: String, securedParamName: String)
  def validate(action: Action, paramName: String, securedParamName: String): Boolean
}
