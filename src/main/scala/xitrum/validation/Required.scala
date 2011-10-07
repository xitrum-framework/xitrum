package xitrum.validation

import scala.xml.{Attribute, Elem, Null, Text}
import xitrum.Action

/** Class "required" will be added to the element. */
object Required extends Validator {
  def render(action: Action, elem: Elem, paramName: String, secureParamName: String): Elem = {
    import action._
    jsAddToView(js$name(secureParamName) + ".rules('add', {required: true})")

    // jQuery Validation plugin does not automatically adds class "required"
    val klass1 = (elem \ "@class").text
    val klass2 = if (klass1.endsWith(" ")) klass1 + "required" else klass1 + " required"
    elem % Attribute(None, "class", Text(klass2), Null)
  }

  def validate(action: Action, paramName: String, secureParamName: String): Boolean = {
    val value = action.param(paramName).trim
    !value.isEmpty
  }
}
