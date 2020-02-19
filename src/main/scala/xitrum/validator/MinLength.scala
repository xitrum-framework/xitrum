package xitrum.validator

object MinLength {
  def apply(length: Int) = new MinLength(length)
}

class MinLength(min: Int) extends Validator[String] {
  def check(value: String): Boolean = value.length >= min

  def message(name: String, value: String): Option[String] =
    if (value.length >= min)
      None
    else
      Some("%s must be at least %d characters".format(name, min))
}
