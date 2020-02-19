package xitrum.validator

object Required extends Validator[String] {
  def check(value: String): Boolean = value.trim.nonEmpty

  def message(name: String, value: String): Option[String] =
    if (value.trim.isEmpty)
      Some("%s must not be empty".format(name))
    else
      None
}
