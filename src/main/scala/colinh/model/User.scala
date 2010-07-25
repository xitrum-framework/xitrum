package colinh.model

import org.squeryl.PrimitiveTypeMode._

class User(
    var id:       Long,
    var username: String) {
  def this() = this(0, "")
}
