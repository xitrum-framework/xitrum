package xitrum.validator

import scala.xml.Elem
import xitrum.Controller

object Min {
  def apply(value: Double) = new Min(value)
}

class Min(value: Double) extends Validator {
  def render(controller: Controller, elem: Elem, paramName: String, secureParamName: String): Elem = {
    import controller._
    jsAddToView(js$name(secureParamName) + ".rules('add', {min: " + value + "})")
    elem
  }

  def validate(controller: Controller, paramName: String, secureParamName: String): Boolean = {
    try {
      val value2 = controller.param(paramName).trim.toDouble
      value2 >= value
    } catch {
      case _ => false
    }
  }
}
