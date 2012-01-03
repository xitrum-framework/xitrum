package xitrum.validator

import scala.xml.Elem
import xitrum.Controller

object Range {
  def apply(min: Double, max: Double) = new Range(min, max)
}

class Range(min: Double, max: Double) extends Validator {
  def render(controller: Controller, elem: Elem, paramName: String, secureParamName: String): Elem = {
    import controller._
    jsAddToView(js$name(secureParamName) + ".rules('add', {range: [" + min + ", " + max + "]})")
    elem
  }

  def validate(controller: Controller, paramName: String, secureParamName: String): Boolean = {
    try {
      val value = controller.param(paramName).trim.toInt
      min <= value && value <= max
    } catch {
      case _ => false
    }
  }
}
