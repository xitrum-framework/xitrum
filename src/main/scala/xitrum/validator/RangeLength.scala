package xitrum.validator

object RangeLength {
  def apply(min: Int, max: Int) = new RangeLength(min, max)
}

class RangeLength(min: Int, max: Int) extends Validator {
  def v(name: String, value: Any) = {
    val value2 = value.asInstanceOf[String].length
    if (min <= value2 && value2 <= max)
      None
    else
      Some("%s must be at least %d and at most %d characters".format(name, min, max))
  }
}
