package xitrum

import xitrum.scope.request.At

class RequestVar[+A](implicit m: Manifest[A]) extends OptVar[A] {
  def getAll(implicit action: Action): At = action.at
}
