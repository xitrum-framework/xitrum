package xitrum

class RequestVar[+A](implicit m: Manifest[A]) extends OptVar[A] {
  def getAll(implicit action: Action) = action.at
}
