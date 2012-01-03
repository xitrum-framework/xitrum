package xitrum.validator

import scala.xml.Elem
import xitrum.Controller

object Email extends Validator {
  def render(controller: Controller, elem: Elem, paramName: String, secureParamName: String): Elem = {
    import controller._
    jsAddToView(js$name(secureParamName) + ".rules('add', {email: true})")
    elem
  }

  def validate(controller: Controller, paramName: String, secureParamName: String): Boolean = {
    val value = controller.param(paramName).trim
    """(?i)^[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,4}$""".r.findFirstIn(value).isDefined
  }
}
