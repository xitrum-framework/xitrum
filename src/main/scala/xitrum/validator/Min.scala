package xitrum.validator

object Min {
  def apply[T <: Comparable[T]](value: T) = new Min(value)

  def apply(value: Byte)   = new Min(new java.lang.Byte(value))
  def apply(value: Double) = new Min(new java.lang.Double(value))
  def apply(value: Float)  = new Min(new java.lang.Float(value))
  def apply(value: Int)    = new Min(new java.lang.Integer(value))
  def apply(value: Long)   = new Min(new java.lang.Long(value))
  def apply(value: Short)  = new Min(new java.lang.Short(value))
}

class Min[T <: Comparable[T]](min: T) extends Validator[T] {
  def check(value: T) = value.compareTo(min) >= 0

  def message(name: String, value: T) =
    if (value.compareTo(min) >= 0)
      None
    else
      Some("%s must be greater than or equal to %s".format(name, min.toString))
}
