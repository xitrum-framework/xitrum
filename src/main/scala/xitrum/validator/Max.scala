package xitrum.validator

object Max {
  def apply(value: Double) = new Max(value)
}

class Max(max: Double) extends Validator {
  def v(name: String, value: Any) =
    if (value.asInstanceOf[Double] <= max)
      None
    else
      Some("%s must be less than or equal to %f".format(name, max))
}
