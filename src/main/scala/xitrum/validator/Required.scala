package xitrum.validator

object Required extends Validator[String] {
  def check(value: String) = !value.trim.isEmpty

  def message(name: String, value: String) =
    if (value.trim.isEmpty)
      Some("%s must not be empty".format(name))
    else
      None
}
