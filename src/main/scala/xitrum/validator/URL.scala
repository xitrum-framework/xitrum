package xitrum.validator

object URL extends Validator {
  def v(name: String, value: Any) =
    if (value.asInstanceOf[String].contains("://"))
      None
    else
      Some("%s must be a URL".format(name))
}
