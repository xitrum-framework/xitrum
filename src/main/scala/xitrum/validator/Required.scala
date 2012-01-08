package xitrum.validator

object Required extends Validator {
  def v(name: String, value: Any) =
    if (value.asInstanceOf[String].trim.isEmpty)
      Some("%s must not be empty".format(name))
    else
      None
}
