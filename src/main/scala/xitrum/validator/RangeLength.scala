package xitrum.validator

object RangeLength {
  def apply(min: Int, max: Int) = new RangeLength(min, max)
}

class RangeLength(min: Int, max: Int) extends Validator[String] {
  def check(value: String) = {
    val length = value.length
    min <= length && length <= max
  }

  def message(name: String, value: String) = {
    val length = value.length
    if (min <= length && length <= max)
      None
    else
      Some("%s must be at least %d and at most %d characters".format(name, min, max))
  }
}
