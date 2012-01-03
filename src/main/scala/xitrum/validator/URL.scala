package xitrum.validator

import scala.xml.Elem
import xitrum.Controller

object URL extends Validator {
  def render(controller: Controller, elem: Elem, paramName: String, secureParamName: String): Elem = {
    import controller._
    jsAddToView(js$name(secureParamName) + ".rules('add', {url: true})")
    elem
  }

  def validate(controller: Controller, paramName: String, secureParamName: String): Boolean = {
    val value = controller.param(paramName).trim
    value.contains("://")
  }
}
