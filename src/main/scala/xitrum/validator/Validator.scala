package xitrum.validator

class ValidationError(message: String) extends Error(message)

trait Validator {
  def v(name: String, value: Any): Option[String]

  def e(name: String, value: Any) {
    v(name, value) match {
      case None =>
      case Some(message) => throw new ValidationError(message)
    }
  }
}
