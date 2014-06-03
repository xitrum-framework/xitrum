package xitrum.validator

object Range {
  def apply[T <: Comparable[T]](min: T, max: T) = new Range(min, max)

  def apply(min: Byte,   max: Byte)   = new Range(new java.lang.Byte(min),    new java.lang.Byte(max))
  def apply(min: Double, max: Double) = new Range(new java.lang.Double(min),  new java.lang.Double(max))
  def apply(min: Float,  max: Float)  = new Range(new java.lang.Float(min),   new java.lang.Float(max))
  def apply(min: Int,    max: Int)    = new Range(new java.lang.Integer(min), new java.lang.Integer(max))
  def apply(min: Long,   max: Long)   = new Range(new java.lang.Long(min),    new java.lang.Long(max))
  def apply(min: Short,  max: Short)  = new Range(new java.lang.Short(min),   new java.lang.Short(max))
}

class Range[T <: Comparable[T]](min: T, max: T) extends Validator[T] {
  def check(value: T) = value.compareTo(min) >= 0 && value.compareTo(max) <= 0

  def message(name: String, value: T) =
    if (value.compareTo(min) >= 0 && value.compareTo(max) <= 0)
      None
    else
      Some("%s must be in range from %s to %s".format(name, min.toString, max.toString))
}
