package xitrum.validator

object MinLength {
  def apply(length: Int) = new MinLength(length)
}

class MinLength(min: Int) extends Validator {
  def v(name: String, value: Any) =
    if (value.asInstanceOf[String].length >= min)
      None
    else
      Some("%s must be at least %d characters".format(name, min))
}
