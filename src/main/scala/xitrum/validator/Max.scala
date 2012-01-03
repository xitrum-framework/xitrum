package xitrum.validator

import scala.xml.Elem
import xitrum.Controller

object Max {
  def apply(value: Double) = new Max(value)
}

class Max(value: Double) extends Validator {
  def render(controller: Controller, elem: Elem, paramName: String, secureParamName: String): Elem = {
    import controller._
    jsAddToView(js$name(secureParamName) + ".rules('add', {max: " + value + "})")
    elem
  }

  def validate(controller: Controller, paramName: String, secureParamName: String): Boolean = {
    try {
      val value2 = controller.param(paramName).trim.toDouble
      value2 <= value
    } catch {
      case _ => false
    }
  }
}
