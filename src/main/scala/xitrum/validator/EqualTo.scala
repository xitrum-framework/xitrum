package xitrum.validator

object EqualTo {
  def apply(name: String, value: Any) = new EqualTo(name, value)
}

class EqualTo(name: String, value: Any) extends Validator {
  def v(name2: String, value2: Any) =
    if (value2 == value)
      None
    else
      Some("%s and %s do not match".format(name2, name))
}
