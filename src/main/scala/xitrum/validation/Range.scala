package xitrum.validation

import scala.xml.Elem
import xitrum.Action

object Range {
  def apply(min: Double, max: Double) = new Range(min, max)
}

class Range(min: Double, max: Double) extends Validator {
  def render(action: Action, elem: Elem, paramName: String, secureParamName: String): Elem = {
    import action._
    jsAddToView(js$name(secureParamName) + ".rules('add', {range: [" + min + ", " + max + "]})")
    elem
  }

  def validate(action: Action, paramName: String, secureParamName: String): Boolean = {
    try {
      val value = action.param(paramName).trim.toInt
      min <= value && value <= max
    } catch {
      case _ => false
    }
  }
}
