package xitrum.exception

case class ValidationError(message: String) extends Error(message)
