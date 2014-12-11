package xitrum

class RequestVar[+A] extends OptVar[A] {
  def getAll(implicit action: Action) = action.at
}
