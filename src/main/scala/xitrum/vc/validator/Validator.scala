package xitrum.vc.validator

import java.io.Serializable
import xitrum.Action

trait Validator extends Serializable {
  def render(action: Action, name: String, name2: String)
  def validate(action: Action, name: String, name2: String): Boolean
}
