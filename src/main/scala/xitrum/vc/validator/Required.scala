package xitrum.vc.validator

import xitrum.Action

class Required extends Validator {
  def render(action: Action, name: String, name2: String) {
    import action._

    val js = jsChain(
      jsByName(name2),
      jsCall("rules", "\"add\"", "{required: true}")
    )
    jsAddToView(js)
  }

  def validate(action: Action, name: String, name2: String): Boolean = {
    true
  }
}
