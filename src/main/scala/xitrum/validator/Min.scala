package xitrum.validator

object Min {
  def apply(value: Double) = new Min(value)
}

class Min(min: Double) extends Validator {
  def v(name: String, value: Any) = {
    if (value.asInstanceOf[Double] >= min)
      None
    else
      Some("%s must be greater than or equal to %f".format(name, min))
  }
}
