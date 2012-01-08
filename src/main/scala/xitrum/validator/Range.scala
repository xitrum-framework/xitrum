package xitrum.validator

object Range {
  def apply(min: Double, max: Double) = new Range(min, max)
}

class Range(min: Double, max: Double) extends Validator {
  def v(name: String, value: Any) = {
    val value2 = value.asInstanceOf[Double]
    if (min <= value2 && value2 <= max)
      None
    else
      Some("%s must not be less than %f or greater than %f".format(name, min, max))
  }
}
