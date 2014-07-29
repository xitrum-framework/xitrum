package xitrum.validator

object Email extends Validator[String] {
  // http://www.w3.org/TR/html-markup/input.email.html
  private val PATTERN = """^[a-zA-Z0-9.!#$%&’*+/=?^_`{|}~-]+@[a-zA-Z0-9-]+(?:\.[a-zA-Z0-9-]+)*$""".r

  def check(value: String) =
    PATTERN.findFirstIn(value).isDefined

  def message(name: String, value: String) =
    if (check(value))
      None
    else
      Some("%s must be an email address".format(name))
}
