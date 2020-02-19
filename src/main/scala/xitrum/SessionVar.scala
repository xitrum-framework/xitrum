package xitrum

import scala.collection.mutable

class SessionVar[+A](implicit m: Manifest[A]) extends OptVar[A] {
  def getAll(implicit action: Action): mutable.Map[String, Any] = action.session
}
