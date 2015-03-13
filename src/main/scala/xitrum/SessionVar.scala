package xitrum

class SessionVar[+A](implicit m: Manifest[A]) extends OptVar[A] {
  def getAll(implicit action: Action) = action.session
}
