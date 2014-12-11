package xitrum

class SessionVar[+A] extends OptVar[A] {
  def getAll(implicit action: Action) = action.session
}
