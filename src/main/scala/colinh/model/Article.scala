package colinh.model

import org.squeryl.PrimitiveTypeMode._

class Article (
    var id:     Long,
    var title:  String,
    var teaser: String,
    var body:   String,
    var userId: Long) {
  def this() = this(0, "", "", "", 0)
}

object Article {
  def all = from(Schema.articles)(a => select(a))
}
