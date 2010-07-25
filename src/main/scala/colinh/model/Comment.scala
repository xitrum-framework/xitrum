package colinh.model

import org.squeryl.PrimitiveTypeMode._

class Comment(
    var id:        Long,
    var articleId: Long,
    var body:      String,
    var userId:    Long) {
  def this() = this(0, 0, "", 0)
}
