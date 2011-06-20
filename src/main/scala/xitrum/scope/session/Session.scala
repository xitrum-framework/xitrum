package xitrum.scope.session

trait Session {
  // Ensure that an exception is thrown, not null
  def apply[T](key: String): T = {
    if (isDefinedAt(key)) {
      val value = get[T](key)
      if (value == null) {
        throw new Exception("No \"" + key + "\" in session")
      } else {
        value
      }
    } else {
      throw new Exception("No \"" + key + "\" in session")
    }
  }

  def update(key: String, value: Any)

  def get[T](key: String): T
  def isDefinedAt(key: String): Boolean
  def delete(key: String)
  def reset
}
