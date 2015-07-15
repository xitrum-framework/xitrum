package xitrum.validator

object Max {
  def apply[T <: Comparable[T]](value: T) = new Max(value)

  def apply(value: Byte)   = new Max(new java.lang.Byte(value))
  def apply(value: Double) = new Max(new java.lang.Double(value))
  def apply(value: Float)  = new Max(new java.lang.Float(value))
  def apply(value: Int)    = new Max(new java.lang.Integer(value))
  def apply(value: Long)   = new Max(new java.lang.Long(value))
  def apply(value: Short)  = new Max(new java.lang.Short(value))
}

class Max[T <: Comparable[T]](max: T) extends Validator[T] {
  def check(value: T) = value.compareTo(max) <= 0

  def message(name: String, value: T) =
    if (value.compareTo(max) <= 0)
      None
    else
      Some("%s must be less than or equal to %s".format(name, max.toString))
}
