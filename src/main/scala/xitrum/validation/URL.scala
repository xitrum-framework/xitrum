package xitrum.validation

import scala.xml.Elem
import xitrum.Action

object URL extends Validator {
  def render(action: Action, elem: Elem, paramName: String, secureParamName: String): Elem = {
    import action._
    jsAddToView(js$name(secureParamName) + ".rules('add', {url: true})")
    elem
  }

  def validate(action: Action, paramName: String, secureParamName: String): Boolean = {
    val value = action.param(paramName).trim
    value.contains("://")
  }
}
