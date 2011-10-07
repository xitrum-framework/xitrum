package xitrum.validation

import scala.xml.Elem
import xitrum.Action

object Validated extends Validator {
  def render(action: Action, elem: Elem, paramName: String, secureParamName: String) = elem
  def validate(action: Action, paramName: String, secureParamName: String) = true

  def secureParamName(paramName: String) = ValidatorInjector.injectToParamName(paramName)
}
