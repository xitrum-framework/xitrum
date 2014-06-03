package xitrum.validator

object MaxLength {
  def apply(length: Int) = new MaxLength(length)
}

class MaxLength(max: Int) extends Validator[String] {
  def check(value: String) = value.length <= max

  def message(name: String, value: String) =
    if (value.length <= max)
      None
    else
      Some("%s must not be longer than %d characters".format(name, max))
}
