package xitrum.validator

import xitrum.exception.InvalidInput

trait Validator[T] {
  /** Returns false if validation fails, otherwise true. */
  def check(value: T): Boolean

  /** Returns Some(error message) if validation fails, otherwise None. */
  def message(name: String, value: T): Option[String]

  /** Throws exception InvalidInput(error message) if validation fails. */
  def exception(name: String, value: T) {
    message(name, value).foreach { message => throw new InvalidInput(message) }
  }
}
