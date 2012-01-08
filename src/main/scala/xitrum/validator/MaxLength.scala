package xitrum.validator

object MaxLength {
  def apply(length: Int) = new MaxLength(length)
}

class MaxLength(max: Int) extends Validator {
  def v(name: String, value: Any) =
    if (value.asInstanceOf[String].length <= max)
      None
    else
      Some("%s must not be longer than %d characters".format(name, max))
}
