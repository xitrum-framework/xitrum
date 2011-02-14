package xitrum.vc.validator

import xitrum.Action

class URL extends Validator {
  def render(action: Action, name: String, name2: String) {
    import action._

    val js = jsChain(
      jsByName(name2),
      jsCall("rules", "\"add\"", "{url: true}")
    )
    jsAddToView(js)
  }

  def validate(action: Action, name: String, name2: String): Boolean = {
    true
  }
}
