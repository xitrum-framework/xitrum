package colinh.model

import org.squeryl.PrimitiveTypeMode._

class User(
    var id:       Int,
    var username: String) {
  def this() = this(0, "")
}
