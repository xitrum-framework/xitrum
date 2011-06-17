package xitrum.validation

import scala.xml.Elem
import xitrum.action.Action

class Validate(action: Action, validators: Validator*) {
  import ValidatorInjector._

  def ::(elem: Elem): Elem = {
    val (elem2, paramName, securedParamName) = injectToParamName(elem, validators:_*)
    for (v <- validators) v.render(action, paramName, securedParamName)
    elem2
  }
}

object Validate {
  def apply(validators: Validator*)(implicit action: Action): Validate = new Validate(action, validators:_*)
}
