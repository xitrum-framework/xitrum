package xitrum.validator

object Url extends Validator[String] {
  def check(value: String) = value.contains("://")

  def message(name: String, value: String) =
    if (value.contains("://"))
      None
    else
      Some("%s must be a URL".format(name))
}
