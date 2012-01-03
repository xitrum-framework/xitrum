package xitrum.validator

import scala.xml.Elem
import xitrum.Controller

object RangeLength {
  def apply(min: Int, max: Int) = new RangeLength(min, max)
}

class RangeLength(min: Int, max: Int) extends Validator {
  def render(controller: Controller, elem: Elem, paramName: String, secureParamName: String): Elem = {
    import controller._
    jsAddToView(js$name(secureParamName) + ".rules('add', {rangelength: [" + min + ", " + max + "]})")
    elem
  }

  def validate(controller: Controller, paramName: String, secureParamName: String): Boolean = {
    try {
      val value = controller.param(paramName).trim.length
      min <= value && value <= max
    } catch {
      case _ => false
    }
  }
}
