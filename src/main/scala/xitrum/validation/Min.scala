package xitrum.validation

import scala.xml.Elem
import xitrum.Action

object Min {
  def apply(value: Double) = new Min(value)
}

class Min(value: Double) extends Validator {
  def render(action: Action, elem: Elem, paramName: String, secureParamName: String): Elem = {
    import action._
    jsAddToView(js$name(secureParamName) + ".rules('add', {min: " + value + "})")
    elem
  }

  def validate(action: Action, paramName: String, secureParamName: String): Boolean = {
    try {
      val value2 = action.param(paramName).trim.toDouble
      value2 >= value
    } catch {
      case _ => false
    }
  }
}
