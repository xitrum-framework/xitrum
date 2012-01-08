package xitrum.validator

object Email extends Validator {
  def v(name: String, value: Any) =
    if ("""(?i)^[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,4}$""".r.findFirstIn(value.asInstanceOf[String]).isDefined)
      None
    else
      Some("%s must be an email address".format(name))
}
