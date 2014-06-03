package xitrum.validator

object Email extends Validator[String] {
  private val PATTERN = """(?i)^[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,4}$""".r

  def check(value: String) =
    PATTERN.findFirstIn(value).isDefined

  def message(name: String, value: String) =
    if (check(value))
      None
    else
      Some("%s must be an email address".format(name))
}
