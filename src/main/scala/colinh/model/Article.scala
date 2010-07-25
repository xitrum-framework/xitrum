package colinh.model

import org.squeryl.PrimitiveTypeMode._
import Schema._

class Article (
    var id:     Long,
    var title:  String,
    var teaser: String,
    var body:   String,
    var userId: Long) {
  def this() = this(0, "", "", "", 0)
}

object Article {
  def all = from(articles)(a => select(a))

  def first(id: Long) = from(articles)(a => where(a.id === id) select(a)).single
}
