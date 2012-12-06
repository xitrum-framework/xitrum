package xitrum.validator

import xitrum.exception.InvalidInput

trait Validator {
  def v(name: String, value: Any): Option[String]

  def e(name: String, value: Any) {
    v(name, value).foreach { message => throw new InvalidInput(message) }
  }
}
